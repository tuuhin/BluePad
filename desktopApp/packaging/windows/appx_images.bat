@echo off
setlocal enabledelayedexpansion

:: Default values
set "SOURCE_FILE="
set "OUTPUT_DIR=appx"
set "BG_COLOR=none"
set "ROUND_RADIUS="
set "FOREGROUND_SCALE=100"
set "GENERATE_SVG_OUT=0"

:: Parse arguments
:parse_args
if "%~1"=="" goto validate_args
if "%~1"=="-c" (
    set "BG_COLOR=%~2"
    shift & shift
    goto parse_args
)
if "%~1"=="--color" (
    set "BG_COLOR=%~2"
    shift & shift
    goto parse_args
)
if "%~1"=="-f" (
    set "FOREGROUND_SCALE=%~2"
    shift & shift
    goto parse_args
)
if "%~1"=="--foreground" (
    set "FOREGROUND_SCALE=%~2"
    shift & shift
    goto parse_args
)
if "%~1"=="-r" (
    if "%~2"=="" (
        set "ROUND_RADIUS=24"
        shift
    ) else (
        echo %~2 | findstr /R "^[0-9]" >nul
        if errorlevel 1 (
            set "ROUND_RADIUS=24"
            shift
        ) else (
            set "ROUND_RADIUS=%~2"
            shift & shift
        )
    )
    goto parse_args
)
if "%~1"=="--rounded" (
    if "%~2"=="" (
        set "ROUND_RADIUS=24"
        shift
    ) else (
        echo %~2 | findstr /R "^[0-9]" >nul
        if errorlevel 1 (
            set "ROUND_RADIUS=24"
            shift
        ) else (
            set "ROUND_RADIUS=%~2"
            shift & shift
        )
    )
    goto parse_args
)
if "%~1"=="-s" (
    set "GENERATE_SVG_OUT=1"
    shift
    goto parse_args
)
if "%~1"=="--svg" (
    set "GENERATE_SVG_OUT=1"
    shift
    goto parse_args
)

:: Catch positional files and folders
if "%SOURCE_FILE%"=="" (
    set "SOURCE_FILE=%~1"
    shift
    goto parse_args
)
if "%OUTPUT_DIR%"=="appx" (
    set "OUTPUT_DIR=%~1"
    shift
    goto parse_args
)
shift
goto parse_args

:validate_args
if "%SOURCE_FILE%"=="" (
    echo Usage: %~nx0 ^<icon.png^|icon.svg^> [output-directory] [options]
    echo Options:
    echo   -c, --color ^<color^>        Background color ^(e.g. "#C4F18C", blue, none^)
    echo   -f, --foreground ^<scale^>   Foreground icon scale percentage ^(e.g. 70, 80^). Default: 100
    echo   -r, --rounded [radius]     Clip and round corners. Defaults to 24.
    echo   -s, --svg                  Generate a processed output SVG along with the ICO
    exit /b 1
)

if not exist "%SOURCE_FILE%" (
    echo Error: File not found: %SOURCE_FILE%
    exit /b 1
)

:: Check extension
for %%I in ("%SOURCE_FILE%") do set "EXT=%%~xI"
set "EXT_LOWER=%EXT:.=%"
if /I not "%EXT_LOWER%"=="png" if /I not "%EXT_LOWER%"=="svg" (
    echo Error: Input must be a .png or .svg file
    exit /b 1
)

:: Check for ImageMagick
where magick >nul 2>nul
if errorlevel 1 (
    echo Error: ImageMagick ^(magick^) not found in PATH
    exit /b 1
)

:: Setup Directory Structures
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
for %%I in ("%OUTPUT_DIR%") do set "ICO_OUTPUT_DIR=%%~dpI"
for %%I in ("%SOURCE_FILE%") do set "ICO_BASE_NAME=%%~nI"

:: Strip trailing backslash from parent directory
if "%ICO_OUTPUT_DIR:~-1%"=="\" set "ICO_OUTPUT_DIR=%ICO_OUTPUT_DIR:~0,-1%"

:: -------------------------------------------------------------------------
:: Step 1: Prepare High-Res Intermediate Master Image
:: -------------------------------------------------------------------------
set "MASTER_PNG=%TEMP%\master_icon_%RANDOM%.png"

if /I "%EXT_LOWER%"=="svg" (
    echo Converting SVG to intermediate high-res Master PNG...
    magick -density 300 -background none "%SOURCE_FILE%" "%MASTER_PNG%"
) else (
    set "MASTER_PNG=%SOURCE_FILE%"
)

:: -------------------------------------------------------------------------
:: Step 2: Asset Generation
:: -------------------------------------------------------------------------
echo Background color set to: %BG_COLOR%
echo Foreground scale set to: %FOREGROUND_SCALE%%%
if not "%ROUND_RADIUS%"=="" (
    echo Clip mode enabled ^(Base Radius: %ROUND_RADIUS%px for 256x256^)
) else (
    echo Clip mode disabled ^(Square boundaries^)
)
echo Generating AppX/MSIX assets...

call :generate_logo 44  44  "Square44x44Logo"
call :generate_logo 150 150 "Square150x150Logo"
call :generate_logo 50  50  "StoreLogo"
call :generate_logo 310 150 "Wide310x150Logo"

echo.
echo Generating crisp Windows executable icon outside of the appx folder...
call :generate_ico

if "%GENERATE_SVG_OUT%"=="1" if /I "%EXT_LOWER%"=="svg" (
    call :generate_final_svg
)

:: Clean up temporary master file if one was generated
if not "%MASTER_PNG%"=="%SOURCE_FILE%" if exist "%MASTER_PNG%" del /f /q "%MASTER_PNG%"

echo.
echo Assets written to:
echo   PNGs -^> %OUTPUT_DIR%
echo   ICO  -^> %ICO_OUTPUT_DIR%\%ICO_BASE_NAME%.ico
if "%GENERATE_SVG_OUT%"=="1" if /I "%EXT_LOWER%"=="svg" echo   SVG  -^> %ICO_OUTPUT_DIR%\%ICO_BASE_NAME%.svg
exit /b 0

:: -------------------------------------------------------------------------
:: Subroutines
:: -------------------------------------------------------------------------
:generate_logo
set "W=%~1"
set "H=%~2"
set "NAME=%~3"
set "OUT_PATH=%OUTPUT_DIR%\%NAME%.png"

:: Calculate scaled foreground size
set /a "FG_W=(%W% * %FOREGROUND_SCALE%) / 100"
set /a "FG_H=(%H% * %FOREGROUND_SCALE%) / 100"

if not "%ROUND_RADIUS%"=="" (
    set /a "CALC_RAD=(%ROUND_RADIUS% * %W%) / 256"
    if !CALC_RAD! LSS 1 set "CALC_RAD=1"
    set /a "MAX_X=%W%-1"
    set /a "MAX_Y=%H%-1"

    magick -size %W%x%H% canvas:"%BG_COLOR%" ^
        ^( "%MASTER_PNG%" -filter Lanczos -resize !FG_W!x!FG_H! ^) ^
        -gravity center -composite ^
        ^( +clone -alpha transparent -background none -draw "fill white roundrectangle 0,0 !MAX_X!,!MAX_Y! !CALC_RAD!,!CALC_RAD!" ^) ^
        -compose DstIn -composite -colorspace sRGB "%OUT_PATH%"
) else (
    magick -size %W%x%H% canvas:"%BG_COLOR%" ^
        ^( "%MASTER_PNG%" -filter Lanczos -resize !FG_W!x!FG_H! ^) ^
        -gravity center -composite -colorspace sRGB "%OUT_PATH%"
)
echo Generated %NAME%.png ^(%W%x%H%^)
exit /b 0

:generate_ico
set "OUT_PATH=%ICO_OUTPUT_DIR%\%ICO_BASE_NAME%.ico"

set "ICO_SIZES=256 48 32 16"
set "ICO_FILES="

for %%S in (%ICO_SIZES%) do (
    set "LAYER_PNG=%TEMP%\ico_layer_%%S_%RANDOM%.png"
    set "ICO_FILES=!ICO_FILES! "!LAYER_PNG!""

    set /a "FG_S=(%%S * %FOREGROUND_SCALE%) / 100"

    if not "%ROUND_RADIUS%"=="" (
        set /a "CALC_RAD=(%ROUND_RADIUS% * %%S) / 256"
        if !CALC_RAD! LSS 1 set "CALC_RAD=1"
        set /a "MAX_DIM=%%S-1"

        magick -size %%Sx%%S canvas:"%BG_COLOR%" ^
            ^( "%MASTER_PNG%" -filter Lanczos -resize !FG_S!x!FG_S! ^) ^
            -gravity center -composite ^
            ^( +clone -alpha transparent -background none -draw "fill white roundrectangle 0,0 !MAX_DIM!,!MAX_DIM! !CALC_RAD!,!CALC_RAD!" ^) ^
            -compose DstIn -composite -colorspace sRGB "!LAYER_PNG!"
    ) else (
        magick -size %%Sx%%S canvas:"%BG_COLOR%" ^
            ^( "%MASTER_PNG%" -filter Lanczos -resize !FG_S!x!FG_S! ^) ^
            -gravity center -composite -colorspace sRGB "!LAYER_PNG!"
    )
)

:: Pack layers into single ICO container
magick !ICO_FILES! "%OUT_PATH%"

:: Clean up temp layer files
for %%F in (!ICO_FILES!) do (
    if exist %%F del /f /q %%F
)

echo Generated %ICO_BASE_NAME%.ico in %ICO_OUTPUT_DIR%
exit /b 0

:generate_final_svg
set "OUT_PATH=%ICO_OUTPUT_DIR%\%ICO_BASE_NAME%.svg"
echo Generating flattened, clipped final vector SVG asset...

set "SVG_RAD=0"
if not "%ROUND_RADIUS%"="" set "SVG_RAD=%ROUND_RADIUS%"

:: Compute SVG internal positioning scale
set /a "SVG_SCALE_VAL=(150 * %FOREGROUND_SCALE%) / 100"
set /a "SVG_TRANS=(256 - ((115 * %FOREGROUND_SCALE%) / 100)) / 2"

(
echo ^<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 256 256" width="256" height="256"^>
echo   ^<defs^>
echo     ^<clipPath id="icon-clip"^>
echo       ^<rect x="0" y="0" width="256" height="256" rx="%SVG_RAD%" ry="%SVG_RAD%" /^>
echo     ^</clipPath^>
echo   ^</defs^>
echo   ^<g clip-path="url(#icon-clip)"^>
echo     ^<rect width="256" height="256" fill="%BG_COLOR%" /^>
echo     ^<g transform="translate(128, 128) scale(0.%FOREGROUND_SCALE%) translate(-57.5, -74)"^>
echo       ^<path fill="#2d527c" d="M10.22 7.18H104.74A10.23 10.23 0 0 1 114.96 17.4V120.3a4.18 4.18 0 0 1-1.23 2.95l-23.43 23.43a4.18 4.18 0 0 1-5.91-5.91l22.2-22.21V17.4a1.86 1.86 0 0 0-1.85-1.86H10.22A1.86 1.86 0 0 0 8.36 17.4v120.18a1.86 1.86 0 0 0 1.86 1.86h58.63a4.18 4.18 0 1 1 0 8.36H10.22A10.23 10.23 0 0 1 0 137.58V17.4A10.23 10.23 0 0 1 10.22 7.18z"/^>
echo       ^<path fill="#2d527c" d="M87.35 117.19v23.43a4.18 4.18 0 0 1-8.36 0v-23.43a4.18 4.18 0 0 1 4.18-4.18h23.43a4.18 4.18 0 0 1 2.96 7.13l-22.21 22.21a4.18 4.18 0 0 1-5.91-5.91l9.15-9.16z"/^>
echo       ^<path fill="#cee8fa" d="M67.1 11.36h100.56a6.04 6.04 0 0 1 6.04 6.04v22.46H61.06V17.4a6.04 6.04 0 0 1 6.04-6.04z" transform="translate(-61.06 -4.18)"/^>
echo       ^<path fill="#2d527c" d="M110.78 44.04H4.18A4.18 4.18 0 0 1 0 39.86V17.4A10.23 10.23 0 0 1 10.22 7.18h94.52A10.23 10.23 0 0 1 114.96 17.4v22.46a4.18 4.18 0 0 1-4.18 4.18zM8.36 35.68h98.24V17.4a1.86 1.86 0 0 0-1.86-1.86H10.22A1.86 1.86 0 0 0 8.36 17.4z"/^>
echo       ^<path fill="#2d527c" d="M21 0a4.18 4.18 0 0 1 4.18 4.18v14.91a4.18 4.18 0 1 1-8.36 0V4.18A4.18 4.18 0 0 1 21 0zm25.33 0a4.18 4.18 0 0 1 4.18 4.18v14.91a4.18 4.18 0 1 1-8.36 0V4.18A4.18 4.18 0 0 1 46.33 0zm25.33 0a4.18 4.18 0 0 1 4.18 4.18v14.91a4.18 4.18 0 1 1-8.36 0V4.18A4.18 4.18 0 0 1 71.66 0zm25.33 0a4.18 4.18 0 0 1 4.18 4.18v14.91a4.18 4.18 0 1 1-8.36 0V4.18A4.18 4.18 0 0 1 96.99 0z"/^>
echo       ^<path fill="#2d527c" d="M21 56.03a4.18 4.18 0 0 1 4.18 4.18v67.04a4.18 4.18 0 1 1-8.36 0V60.21A4.18 4.18 0 0 1 21 56.03z"/^>
echo     ^</g^>
echo   ^</g^>
echo ^</svg^>
) > "%OUT_PATH%"

echo Generated %ICO_BASE_NAME%.svg in %ICO_OUTPUT_DIR%
exit /b 0
