package pojo;

public class CustomAttributes {

    /*
    "value": null,
"custom_attribute_id": 246368,
"custom_attribute_name": "Manufacturer SKU",
     */

    private String value;
    private Float custom_attribute_id;
    private String custom_attribute_name;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Float getCustom_attribute_id() {
        return custom_attribute_id;
    }

    public void setCustom_attribute_id(Float custom_attribute_id) {
        this.custom_attribute_id = custom_attribute_id;
    }

    public String getCustom_attribute_name() {
        return custom_attribute_name;
    }

    public void setCustom_attribute_name(String custom_attribute_name) {
        this.custom_attribute_name = custom_attribute_name;
    }

    @Override
    public String toString() {
        return "CustomAttributes{" +
                "value='" + value + '\'' +
                ", custom_attribute_id=" + custom_attribute_id +
                ", custom_attribute_name='" + custom_attribute_name + '\'' +
                '}';
    }

    public String customAttributesPayload(){

    return"{\n" +
            "                    \"value\": "+value+",\n" +
            "                    \"custom_attribute_id\": "+custom_attribute_id+"\n" +
            "                }";
    }
}
