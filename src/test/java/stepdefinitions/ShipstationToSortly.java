package stepdefinitions;
import Utilities.ConfigReader;
import Utilities.DBUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import pojo.GetAllSortlyItems;
import pojo.MainOrders;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.restassured.RestAssured.given;

public class ShipstationToSortly {

    static String date;

    static Response response;
    static MainOrders mainOrders;
    static ObjectMapper obj = new ObjectMapper();
    static GetAllSortlyItems getAllSortlyItem = new GetAllSortlyItems();

    public static void main(String[] args) throws SQLException, JsonProcessingException, ConfigurationException {

       //STEP 1: Get all new orders from shipstation
        // STEP 2: Combine the orders with the same sku
        // STEP 3: Get quantity from Sortly into DATABASE
        // STEP 4: Compare the skus between Shipstation and Sortly and update the new quantity
        // STEP 5: Send the updated quantity to sortly

        String lightning_Shipstation_API_Key="NjQ4ZjJhYjUxNmU0NDk2MmExZTFlY2VmZjIzYzhkYzg6M2Y4MTk0NGViNGUwNGM2MGFhYWY1MTQ0MDg1NDBlZDY=";
        String lmp_Shipstation_Api_Key= null;
        String etr_Shipstation_Api_Key = null;
        String melek_shipstation_Api_Key = null;

        List<String> ls = new ArrayList<>();
        ls.add(lightning_Shipstation_API_Key);
        ls.add(etr_Shipstation_Api_Key);
        ls.add(lmp_Shipstation_Api_Key);
        ls.add(melek_shipstation_Api_Key);


            for (int i =0; i<ls.size();i++){

                if(ls.get(i)!=null){

                    //Get data from ship statition

                    writeOrderToDB(ls.get(i));
                    // Get data from sortly

                    updateSortlyTable();

                    //Update data in the database

                    finalUpdate();

                    //Send new data to sortly
                    updateSortly();


                } else {

                    System.out.println("The api key has not entered");
                }


            }


    }

    public static void writeOrderToDB(String shipstationApiKey) throws JsonProcessingException, SQLException, ConfigurationException {

        System.out.println("Orders are being downloaded from Shipstation");


        date = ConfigReader.getProperty("date");

        RestAssured.baseURI = "https://ssapi.shipstation.com/";

        response = given().header("Authorization", "Basic "+shipstationApiKey)
                .queryParam("createDateStart", date)
                .when().get("/orders");

        mainOrders = obj.readValue(response.asString(), MainOrders.class);

        DBUtils.getConnection();
        DBUtils.getStatement();
        String deleteTable1 = "delete from comingOrders";
        String deleteTable2 = "delete from sortedorders";
        DBUtils.executeInsert(deleteTable1);
        DBUtils.executeInsert(deleteTable2);


        for (int i = 0; i < mainOrders.getOrders().size(); i++) {

            for (int j = 0; j < mainOrders.getOrders().get(i).getItems().size(); j++) {

                String sku = mainOrders.getOrders().get(i).getItems().get(j).getSku();
                int quantity = mainOrders.getOrders().get(i).getItems().get(j).getQuantity();

                if (!(sku == null)) {

                    String s = "insert into comingOrders values('" + sku + "','null'," + quantity + ",'location')";
                    DBUtils.executeInsert(s);
                }
            }
        }

        String readTable = "select * from comingOrders";
        ResultSet rs = DBUtils.executeQuery(readTable);

        HashSet<String> hash = new HashSet<>();

        while (rs.next()) {
            hash.add(rs.getString("sku"));
        }

        //Loop through the hashset to find the sum of each quantity row
        for (String element : hash) {

            String countQty = "select sum(quantity) as sumQuantity from comingOrders where sku='" + element + "'";
            ResultSet rs2 = DBUtils.executeQuery(countQty);

            //calculate the sum and create a new pair of data in the table sortedorders
            if (rs2.next()) {
                int sum = rs2.getInt("sumQuantity");
                String update = "insert into sortedorders values('" + element + "','null'," + sum + ",'null')";
                DBUtils.executeInsert(update);
                rs2.close();
            }

        }

        DBUtils.closeConnection();
        rs.close();

        System.out.println("Shipstation Data transfer is COMPLETE");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        PropertiesConfiguration conf = new PropertiesConfiguration("Configuration.properties");
        conf.setProperty("date", dtf.format(now));
        conf.save();
    }

    public static void updateSortlyTable() throws JsonProcessingException {

        System.out.println("Sortly Data is being PULLED");

        //get all items from sortly under the folder ID=41349829
        response = given().headers("Authorization", "Bearer "+ConfigReader.getProperty("Sortly_Api_Secret_Key"),
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

        DBUtils.closeConnection();

        System.out.println("Sortly Data Transfer is COMPLETE");
    }

    public static void finalUpdate() throws SQLException {

        System.out.println("Inventory Update is ongoing");

        String url = "jdbc:mysql://sql5.freesqldatabase.com:3306/sql5507965";
        String username = "sql5507965";
        String password = "DNweUgzgJI";

        Connection con1 = DriverManager.getConnection(url, username, password);
        Statement stmt1 = con1.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);

        Connection con2 = DriverManager.getConnection(url, username, password);
        Statement stmt2 = con2.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);

        Connection con3 = DriverManager.getConnection(url, username, password);
        Statement stmt3 = con3.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
                ResultSet.CONCUR_UPDATABLE);


        String queryDelete = "delete from finalTable";
        stmt3.executeUpdate(queryDelete);

        String query1 = "select * from sortedorders";
        ResultSet rs1 = stmt1.executeQuery(query1);

        String query2 = "select * from sortlyItems";
        ResultSet rs2 = stmt2.executeQuery(query2);

        int oldqty;
        int qtyDiff;
        int updatedQty;
        int id;
        String sku;

        while(rs2.next()){

            rs1.beforeFirst();
            while(rs1.next()){

                String skuSub=rs1.getString("sku").replaceAll("\\D", "");

                if(!skuSub.equals("") && rs2.getString("sku").contains(skuSub)){

                    oldqty=rs2.getInt("quantity");
                    qtyDiff= rs1.getInt("quantity");
                    updatedQty = oldqty - qtyDiff;

                    id = rs2.getInt("id");
                    sku=rs2.getString("sku");

                    if(updatedQty<=0){

                        updatedQty=0;
                        String query3= "insert into finalTable values("+id+",'"+sku+"',"+updatedQty+")";
                        stmt3.executeUpdate(query3);
                    } else {

                        String query4= "insert into finalTable values("+id+",'"+sku+"',"+updatedQty+")";
                        stmt3.executeUpdate(query4);

                    }

                } else {

                    System.out.println("This product couldn't been found in Sortly database= "+rs1.getString("sku"));
                }
            }

        }

        con1.close();
        con2.close();
        stmt1.close();
        stmt2.close();
        con3.close();
        stmt3.close();

        System.out.println("Data is updated, Ready to Go");

    }

    public static void updateSortly() throws SQLException {

        System.out.println("Sortly is being updated with the new data");

        DBUtils.getConnection();
        DBUtils.getStatement();

        String query = "select * from finalTable";
        ResultSet rs = DBUtils.executeQuery(query);


        while (rs.next()){

            int itemID = rs.getInt("id");
            int quantity = rs.getInt("quantity");

            //Update the new quantity
            response = given().headers("Authorization","Bearer "+ConfigReader.getProperty("Sortly_Api_Secret_Key"),
                            "Content-Type",ContentType.JSON,
                            "Accept",ContentType.JSON).pathParams("item_id",itemID)
                    .body("{\n" +
                            "  \"quantity\":"+quantity+"\n" +
                            "}")
                    .when().put("https://api.sortly.co/api/v1/items/{item_id}");

        }

        DBUtils.closeConnection();


        System.out.println("SUCCESS");


    }

}
