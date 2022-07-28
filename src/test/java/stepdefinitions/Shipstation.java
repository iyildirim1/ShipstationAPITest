package stepdefinitions;

import Utilities.ConfigReader;
import Utilities.DBUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mysql.cj.protocol.Resultset;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.checkerframework.checker.units.qual.Temperature;
import org.junit.Test;
import pojo.GetAllSortlyItems;
import pojo.MainOrders;
import pojo.Options;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import static io.restassured.RestAssured.given;

public class Shipstation {

    Response response;
    ObjectMapper obj = new ObjectMapper();
    MainOrders mainOrders = new MainOrders();

    GetAllSortlyItems getAllSortlyItem = new GetAllSortlyItems();

    @Test
public void getShipstationOrdersNew() throws JsonProcessingException {

   String date = ConfigReader.getProperty("date");

    RestAssured.baseURI = "https://ssapi.shipstation.com/";

    response = given().header("Authorization", "Basic "+ConfigReader.getProperty("LD_ShipStation_Api_Key"))
            .queryParam("createDateStart", date)
            .when().get("/orders");

    mainOrders = obj.readValue(response.asString(), MainOrders.class);

    DBUtils.getConnection();
    DBUtils.getStatement();

    for (int i = 0; i < mainOrders.getOrders().size(); i++) {

        for (int j = 0; j < mainOrders.getOrders().get(i).getItems().size(); j++) {

            int shipstationID = mainOrders.getOrders().get(i).getOrderId();
            String orderNumber=mainOrders.getOrders().get(i).getOrderNumber();
            String sku = mainOrders.getOrders().get(i).getItems().get(j).getSku();
            int quantity = mainOrders.getOrders().get(i).getItems().get(j).getQuantity();
            String imageUrl = mainOrders.getOrders().get(i).getItems().get(j).getImageUrl();
            String optionName;
            String optionValue;

            for(Options option:mainOrders.getOrders().get(i).getItems().get(j).getOptions()){

                     optionName = option.getName();
                     optionValue = option.getValue();

                if (sku != null) {

                    String s = "insert into shipstationOrders values("+shipstationID+",'"+orderNumber+"','"+sku+"',"+quantity+", '"+imageUrl+"', '"+optionName+"', '"+optionValue+"','null','null','null')";
                    DBUtils.executeInsert(s);
                }
            }


        }
    }

        DBUtils.closeConnection();

}

    @Test
    public void getShipstationOrders() throws JsonProcessingException, SQLException {

        String date = "2022/07/20 16:00:49";

        RestAssured.baseURI = "https://ssapi.shipstation.com/";

        response = given().header("Authorization", "Basic " + "NjQ4ZjJhYjUxNmU0NDk2MmExZTFlY2VmZjIzYzhkYzg6M2Y4MTk0NGViNGUwNGM2MGFhYWY1MTQ0MDg1NDBlZDY=")
                .queryParam("createDateStart", date)
                .when().get("/orders");

        mainOrders = obj.readValue(response.asString(), MainOrders.class);


        // Get orders from shipstation and put them in a map <SKU, Quantity>
        HashMap<String, Integer> map = new HashMap<>();

        for (int i = 0; i < mainOrders.getOrders().size(); i++) {

            for (int j = 0; j < mainOrders.getOrders().get(i).getItems().size(); j++) {

                String sku = mainOrders.getOrders().get(i).getItems().get(j).getSku();
                int quantity = mainOrders.getOrders().get(i).getItems().get(j).getQuantity();
                String orderNo = mainOrders.getOrders().get(i).getItems().get(j).getOrderNumber();

                try {

                    if(sku.length()>3){

                        if (map.get(sku) == null) {

                            map.put(sku, quantity);

                        } else {

                            int oldQty = map.get(sku);
                            int sum = oldQty + quantity;

                            map.put(sku, sum);

                        }

                    }



                } catch (NullPointerException npe){

                    System.out.println("There is no SKU for this order :"+orderNo );

                }

            }

        }


        response = given().headers("Authorization", "Bearer "+ ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                        "Accept", ContentType.JSON)
                .when().get("https://api.sortly.co/api/v1/items?per_page=500&folder_id=41349829");

        // Deserialize data into pojo classes
        getAllSortlyItem = obj.readValue(response.asString(), GetAllSortlyItems.class);

        DBUtils.getConnection();
        DBUtils.getStatement();

        // delete all data in sortlyItems Table
        String s = "delete from sortlyItems";
        DBUtils.executeInsert(s);

        String sku;
        int quantity;
        int id;

        for (int i=0; i<getAllSortlyItem.getData().size();i++) {

            sku = getAllSortlyItem.getData().get(i).getName();
            quantity = getAllSortlyItem.getData().get(i).getQuantity().intValue();
            id=getAllSortlyItem.getData().get(i).getId();

            // update the sortlyItems table with fresh data
            String query = "insert into sortlyItems values("+id+",'"+sku+"',"+quantity+")";
            DBUtils.executeInsert(query);
        }

         String query2 = "select * from sortlyItems";
        ResultSet resultSet = DBUtils.executeQuery(query2);

        HashMap<String,Integer> map2 = new HashMap<>();

        while (resultSet.next()){

            map2.put(resultSet.getString("sku"),resultSet.getInt("quantity"));

          }


        for (String str2 : map2.keySet()){

           for(String str3 : map.keySet()){

               if (str2.contains(str3.substring(3))){

                   int oldQty = map2.get(str2);
                   int qtyChange = map.get(str3);
                   int updatedQty = oldQty-qtyChange;

                   map2.put(str2,updatedQty);
               }
           }
        }

        for (String s2: map2.keySet() ){

            System.out.println(s2+"==="+map2.get(s2));
        }

        HashMap<String, Integer> map3 = new HashMap<>();


            for (String s2 : map2.keySet()) {

                String query = "select id from sortlyItems where sku='" + s2 + "'";

                ResultSet resultSet2 = DBUtils.executeQuery(query);

                while (resultSet2.next()) {

                    map3.put(s2, resultSet2.getInt("id"));

                }
            }

        System.out.println("++++++++++++++++++++++++++++++++");
                for (String s4: map3.keySet()){

                    if(map3.get(s4)!=map2.get(s4)){

                        int itemID = map3.get(s4);
                        quantity = map2.get(s4);

                        System.out.println(itemID+"= "+quantity);

                    }



//                    //Update the new quantity
//                    response = given().headers("Authorization","Bearer "+ConfigReader.getProperty("Sortly_Api_Secret_Key"),
//                                    "Content-Type",ContentType.JSON,
//                                    "Accept",ContentType.JSON).pathParams("item_id",itemID)
//                            .body("{\n" +
//                                    "  \"quantity\":"+quantity+"\n" +
//                                    "}")
//                            .when().put("https://api.sortly.co/api/v1/items/{item_id}");
                }

                DBUtils.closeConnection();


    }
}
