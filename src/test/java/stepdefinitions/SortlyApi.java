package stepdefinitions;

import Utilities.ConfigReader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.Test;
import pojo.GetAllSortlyItems;
import pojo.GetJdsInventory;

import java.util.HashMap;

import static io.restassured.RestAssured.*;

public class SortlyApi {

    Response response;
    ObjectMapper obj = new ObjectMapper();

    GetAllSortlyItems getAllSortlyItems;

    StringBuilder sb = new StringBuilder();

    GetJdsInventory getJdsInventory = new GetJdsInventory();

    @Test
    public void getItems() throws JsonProcessingException {

        String folderId = ConfigReader.getProperty("Sortly_Main_Folder_ID");

       response= given().headers("Authorization","Bearer "+ ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                "Accept",ContentType.JSON)
                .when().
                get("https://api.sortly.co/api/v1/items?per_page=1000&folder_id="+folderId+"&include=custom_attributes");

      // getAllSortlyItems = obj.readValue(response.asString(),GetAllSortlyItems.class);

        response.prettyPrint();



    }
    @Test
    public void getItem() throws JsonProcessingException {

        int item_id = 41362985;

        response= given().headers("Authorization","Bearer "+ ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                        "Accept",ContentType.JSON).pathParams("item_id",item_id)
                .queryParam("include","custom_attributes")
                .when().get("https://api.sortly.co/api/v1/items/{item_id}");

        response.prettyPrint();

       // getAllSortlyItems = obj.readValue(response.asString(),GetAllSortlyItems.class);

    }

    @Test
    public void updateManufacturerStock() throws JsonProcessingException {

        System.out.println("Data transfer has started");

        //Get all sortly data and pick the ones with manufacturer sku
        String folderId = ConfigReader.getProperty("Sortly_Main_Folder_ID");

        response= given().headers("Authorization","Bearer "+ ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                        "Accept",ContentType.JSON)
                .when().
                get("https://api.sortly.co/api/v1/items?per_page=1000&folder_id="+folderId+"&include=custom_attributes");

        //Deserialize the json Data
        getAllSortlyItems = obj.readValue(response.asString(),GetAllSortlyItems.class);

        String manufacturerSku;
        int sortlyId;

        // Map the sortly id with the manufacturer sku
        HashMap<String, Integer> map = new HashMap<>();

        //Lopp through the sortly items to find the ones with the manufacturer sku
        for (int i=0; i<getAllSortlyItems.getData().size();i++){

            for (int j=0; j<getAllSortlyItems.getData().get(i).getCustom_attribute_values().size();j++){

                if(getAllSortlyItems.getData().get(i).getCustom_attribute_values().get(j).getValue()!=null &&
                        getAllSortlyItems.getData().get(i).getCustom_attribute_values().get(j).getCustom_attribute_name().equalsIgnoreCase("manufacturer sku"))
                {

                    manufacturerSku=getAllSortlyItems.getData().get(i).getCustom_attribute_values().get(j).getValue();
                    sortlyId=getAllSortlyItems.getData().get(i).getId();
                    map.put(manufacturerSku,sortlyId);

                    // Append the manufacturer SKUs to send in a post request to JDS industries
                    sb.append(manufacturerSku).append(",");

                }

            }

        }

        // Remove "," at the end
        sb.delete((sb.length()-1),sb.length());

        // Search those skus on JDS API
        response= given().header("Content-Type","application/json").body(sb.toString()).
                when().post("https://php.jdsindustries.com/public/api/api_get_data.php?api=api_jds_inventory&num=cAgoEtedwjAtAbuGDmaFfFncvhcnmyv&format=JSON&custNo=93599&custToken=extgdktcesjpxwBdqyerkiArrvsypmn")
                .then().log().all().extract().response();

        // Deserialize the json data
            getJdsInventory = obj.readValue(response.asString(),GetJdsInventory.class);

            String jdsSku;
            int jdsQuantity;

            //NAME AND ID OF THE CUSTOM FIELD OF SORTLY TO BE UPDATED
            int SORTLY_CUSTOMFIELD_ID=246369;
            String SORTLY_CUSTOMFIELD_NAME="Manufacturer Stock";

            for (int i = 0; i < getJdsInventory.getJdsInventory().size();i++){

                jdsSku = getJdsInventory.getJdsInventory().get(i).getProductName();
                jdsQuantity =getJdsInventory.getJdsInventory().get(i).getProductInventory();

                System.out.println(map.get(jdsSku));

                //Update the new quantity with the available skus
                response = given().headers("Authorization","Bearer "+ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                                "Content-Type",ContentType.JSON,
                                "Accept",ContentType.JSON).pathParams("item_id",map.get(jdsSku))
                        .body("{\"custom_attribute_values\": [\n" +
                                "            {\n" +
                                "                \"value\": "+jdsQuantity+",\n" +
                                "                \"custom_attribute_id\": "+SORTLY_CUSTOMFIELD_ID+",\n" +
                                "                \"custom_attribute_name\": \""+SORTLY_CUSTOMFIELD_NAME+"\"\n" +
                                "            }]}")
                        .when().put("https://api.sortly.co/api/v1/items/{item_id}")
                        .then().log().all().extract().response();


            }



    }


}
