package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SortlyItems {

    private int id;
    private String name;
    private String notes;
    private Float price;
    private Float quantity;
    private Float min_quantity;
    private String type;  // important: item or folder
    private Float parent_id; // folder ID
    private List<CustomAttributes> custom_attribute_values;


    public List<CustomAttributes> getCustom_attribute_values() {
        return custom_attribute_values;
    }

    public void setCustom_attribute_values(List<CustomAttributes> custom_attribute_values) {
        this.custom_attribute_values = custom_attribute_values;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public Float getQuantity() {
        return quantity;
    }

    public void setQuantity(Float quantity) {
        this.quantity = quantity;
    }

    public Float getMin_quantity() {
        return min_quantity;
    }

    public void setMin_quantity(Float min_quantity) {
        this.min_quantity = min_quantity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getParent_id() {
        return parent_id;
    }

    public void setParent_id(Float parent_id) {
        this.parent_id = parent_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public  String createItemBody(){
        return "{\n" +
                "  \"name\": \""+name+"\",\n" +
                "  \"quantity\":"+quantity+",\n" +
                "  \"type\": \""+type+"\",\n" +
                "  \"parent_id\": "+parent_id+"\n" +
                "}";

}
}
