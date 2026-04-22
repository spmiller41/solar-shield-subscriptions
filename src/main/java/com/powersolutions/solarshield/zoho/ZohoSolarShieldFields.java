package com.powersolutions.solarshield.zoho;

public final class ZohoSolarShieldFields {

    private ZohoSolarShieldFields() {
        // prevent instantiation
    }

    // Zoho Data Wrapper
    public static final String DATA = "data";
    public static final String RECORD_ID = "id";

    // Account / Core
    public static final String NAME = "Name";
    public static final String ACCOUNT_STATUS = "Account_Status";
    public static final String ACTIVATED_AT = "Activated_At";
    public static final String PLAN = "Plan";
    public static final String ACCOUNT_ID = "Account_ID";

    // Contact
    public static final String FIRST_NAME = "First_Name";
    public static final String LAST_NAME = "Last_Name";
    public static final String EMAIL = "Email";
    public static final String PHONE = "Phone";

    // Address
    public static final String STREET = "Address_Street_Address";
    public static final String CITY = "Address_City";
    public static final String STATE = "Address_State_Province";
    public static final String ZIP = "Address_Zip_Postal_Code";
    public static final String COUNTRY = "Address_Country_Region";

    // Invoice Subform
    public static final String ACCOUNT_INVOICES = "Account_Invoices";
    public static final String INVOICE_AMOUNT = "Amount";
    public static final String INVOICE_STATUS = "Status";
    public static final String INVOICE_UPDATED_AT = "Updated_At";
    public static final String INVOICE_ORDER_ID = "Order_Id";

}
