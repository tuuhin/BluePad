package com.sam.ble_common;

import java.util.ArrayList;
import java.util.List;

public record Characteristic(
        String uuid,
        List<Descriptor> descriptors,
        boolean canRead,
        boolean canWriteRequest,
        boolean canWriteCommand,
        boolean canNotify,
        boolean canIndicate) {

    public static Builder builder(String uuid) {
        return new Builder(uuid);
    }

    public static class Builder {

        private final String uuid;
        private final List<Descriptor> descriptors = new ArrayList<>();
        private boolean canRead = false;
        private boolean canWriteRequest = false;
        private boolean canWriteCommand = false;
        private boolean canNotify = false;
        private boolean canIndicate = false;

        public Builder(String uuid) {
            this.uuid = uuid;
        }

        public Builder addDescriptor(Descriptor descriptor) {
            this.descriptors.add(descriptor);
            return this;
        }

        public Builder canRead(boolean value) {
            this.canRead = value;
            return this;
        }

        public Builder canWriteRequest(boolean value) {
            this.canWriteRequest = value;
            return this;
        }

        public Builder canWriteCommand(boolean value) {
            this.canWriteCommand = value;
            return this;
        }

        public Builder canNotify(boolean value) {
            this.canNotify = value;
            return this;
        }

        public Builder canIndicate(boolean value) {
            this.canIndicate = value;
            return this;
        }

        public Characteristic build() {
            return new Characteristic(
                    uuid,
                    List.copyOf(descriptors),
                    canRead,
                    canWriteRequest,
                    canWriteCommand,
                    canNotify,
                    canIndicate
            );
        }
    }
}