package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    private UnstructuredAddress unstructuredAddress;

    public static class Builder {
        private UnstructuredAddress unstructuredAddress;

        public Builder unstructuredAddress(UnstructuredAddress unstructuredAddress) {
            this.unstructuredAddress = unstructuredAddress;
            return this;
        }

        public Address build() {
            return new Address(unstructuredAddress);
        }
    }

    private Address(UnstructuredAddress unstructuredAddress) {
        this.unstructuredAddress = unstructuredAddress;
    }

    public Address(){}

    public UnstructuredAddress getUnstructuredAddress() {
        return unstructuredAddress;
    }

}
