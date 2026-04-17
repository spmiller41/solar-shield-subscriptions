package com.powersolutions.solarshield.zoho;

public enum ModuleApiName {

    SOLAR_SHIELD_ACCOUNT("Solar_Shield_Account");

    private final String apiName;

    ModuleApiName(String apiName) {this.apiName = apiName; }

    @Override
    public String toString() { return apiName; }

}