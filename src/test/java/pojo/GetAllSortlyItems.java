package pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GetAllSortlyItems {

    private List<SortlyItems> data;

    public List<SortlyItems> getData() {
        return data;
    }

    public void setData(List<SortlyItems> data) {
        this.data = data;
    }


}
