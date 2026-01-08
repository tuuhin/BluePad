package com.sam.bluepad.domain.use_cases

class RandomNameGenerator {

	private val adjectives = listOf(
		"gentle", "silent", "brave", "cosmic", "sunny",
		"frosty", "hidden", "lucky", "mystic", "swift"
	)

	private val nouns = listOf(
		"mountain", "butterfly", "horizon", "riverstone",
		"firestorm", "wildflower", "thunderbird",
		"dreamcatcher", "starlight", "cloudrider"
	)

	fun generateName(): String = "${adjectives.random()}-${nouns.random()}"
}