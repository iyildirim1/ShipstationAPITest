package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetJdsInventory {

    private List<JdsInventory> jdsInventory;

    public List<JdsInventory> getJdsInventory() {
        return jdsInventory;
    }

    public void setJdsInventory(List<JdsInventory> jdsInventory) {
        this.jdsInventory = jdsInventory;
    }


}
