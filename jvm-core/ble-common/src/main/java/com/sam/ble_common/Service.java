package com.sam.ble_common;

import java.util.ArrayList;
import java.util.List;

public record Service(String uuid, byte[] data, List<Characteristic> characteristics) {

    public static Builder builder(String uuid) {
        return new Builder(uuid);
    }

    public static class Builder {

        private final String uuid;
        private final List<Characteristic> characteristics = new ArrayList<>();
        private byte[] data = new byte[0];

        public Builder(String uuid) {
            this.uuid = uuid;
        }

        public Builder addCharacteristic(Characteristic characteristic) {
            this.characteristics.add(characteristic);
            return this;
        }

        public Builder addCharacteristics(List<Characteristic> characteristics) {
            this.characteristics.addAll(characteristics);
            return this;
        }

        public Builder setData(byte[] value) {
            this.data = value;
            return this;
        }

        public Service build() {
            return new Service(
                    uuid,
                    data,
                    List.copyOf(characteristics)
            );
        }
    }
}