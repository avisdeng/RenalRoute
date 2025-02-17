package com.route.plan.util;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.route.plan.entity.Result;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataUtil {
     
     //Load test dataset from file
     public static List<JSONObject> getDataList() {
         List<JSONObject> dataList = new ArrayList<JSONObject>();

//         File file = new File("dataset.json");
         FileReader fileReader = new FileReader("dataset.json");
         String result = fileReader.readString();
         if(StrUtil.isNotBlank(result)) {
             JSONArray jsonArray = JSONUtil.parseArray(result);
             dataList = JSONUtil.toList(jsonArray, JSONObject.class);
         }
         return dataList;
     }

     // Get the city list from DataList
     public static Result<List<String>> getCityList(){
         List<JSONObject> dataList = getDataList();
         List<String> cityList = List.of();
         if(dataList != null && !dataList.isEmpty()) {
             List<String> tmpList = new ArrayList<>();
             dataList.forEach(data -> {
                 tmpList.add(data.getStr("city"));
             });
             cityList = tmpList.stream().distinct().toList();
         }
         return Result.success(cityList);
     }

     // Get the Center info from the selected city
     public static Result<List<JSONObject>> getDataListByCity(String city){
         List<JSONObject> dataList = getDataList();
         List<JSONObject> newDataList = new ArrayList<>();
         if(dataList != null && !dataList.isEmpty()) {
             List<String> tmpList = new ArrayList<>();
             dataList.forEach(data -> {
                  if(data.getStr("city") != null && data.getStr("city").equals(city)) {
                      newDataList.add(data);
                  }
             });
         }
         return Result.success(newDataList);
     }

     // Use Google Gemini to match the plan given the criteras
     public static Result<List<JSONObject>> searchPlan(JSONObject params){

         String promptTemplate = "";
         JSONArray travelCtiyList = params.getJSONArray("travelCtiyList");

         String roundTrip = params.getStr("roundTrip");
         if(roundTrip.equals("no")){

             if(travelCtiyList.size() > 0){

                 final String[] travelCtiy = {""};

                 int i = 0;
                 int finalI = i;
                 travelCtiyList.forEach(item->{
                      if (finalI < travelCtiyList.size() - 1){
                          JSONObject jsonObject = travelCtiyList.getJSONObject(finalI);
                          travelCtiy[0] += StrUtil.format("[{}] [on {}],",jsonObject.getStr("cityName"),jsonObject.getStr("date"));
                      }
                 });
                 i++;

                 params.putOpt("visitCity", travelCtiy[0]);

                 promptTemplate ="A dialysis patient needs to drive from ["+params.getStr("homeCity")+"] to ["+params.getStr("arriveCity")+"]. He plans to depart on ["+params.getStr("homeCityDate")+"] and [arrive] on ["+params.getStr("arriveDate")+"]. He needs to visit "+params.getStr("visitCity")+" along the road. " +
                         "Since he needs dialysis treatment every ["+params.getStr("dialysisCycle")+"] days, he needs to find dialysis centers with available slots along the route. " +
                         "The availability data for dialysis centers is provided in the end. Please help plan his itinerary and list the driving distance for each day. He could drive at most ["+params.getStr("maximumDailyTravelDistance")+"] miles a day. \n" +
                         "return the following json format when the route is found:\n" +
                         "[\n" +
                         "\t{\n" +
                         "\t\t\"date\":\"5/11/25\",\n" +
                         "\t\t\"city\":\"Seattle\",\n" +
                         "\t\t\"center\":\"Center1\",\n" +
                         "\t\t\"timeSlot\":\"08:00-10:00;13:00-17:00\"\n" +
                         "\t},\n" +
                         "\t{\n" +
                         "\t\t\"date\":\"5/12/25\",\n" +
                         "\t\t\"city\":\"Seattle\",\n" +
                         "\t\t\"center\":\"Center2\",\n" +
                         "\t\t\"timeSlot\":\"08:00-10:00\"\n" +
                         "\t}\n" +
                         "]\n" +
                         "\n" +
                         "If there's no routes found, returns empty json string only, no need to explain anything else.\n" +
                         "\n" +
                         "The following are the availability data (json format):\n" +
                         "\n" +
                         "[\n" +
                         "  {\n" +
                         "    \"city\": \"Sacramento\",\n" +
                         "    \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "    \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "    \"contact\": \"326-683-8045\",\n" +
                         "    \"date\": \"2025-02-22\",\n" +
                         "    \"openTimePeriod\": \"07:00-11:00; 13:00-17:00\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"city\": \"Sacramento\",\n" +
                         "    \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "    \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "    \"contact\": \"326-683-8045\",\n" +
                         "    \"date\": \"2025-02-23\",\n" +
                         "    \"openTimePeriod\": \"08:00-12:00; 09:00-13:00\"\n" +
                         "  }\n" +
                         "]";
             }else{
                 promptTemplate = " A dialysis patient needs to drive from ["+params.getStr("homeCity")+"] to ["+params.getStr("arriveCity")+"]. He plans to depart on ["+params.getStr("homeCityDate")+"] and [arrive] on ["+params.getStr("arriveDate")+"].\n" +
                         "                         Since he needs dialysis treatment every ["+params.getStr("dialysisCycle")+"] days, he needs to find dialysis centers with available slots along the route.\n" +
                         "                         The availability data for dialysis centers is provided below. Please help plan his itinerary and list the driving distance for each day.\n" +
                         "                         He could drive at most ["+params.getStr("maximumDailyTravelDistance")+"] miles a day.\n" +
                         "                             return the following json format when the route is found:\n" +
                         "                             [\n" +
                         "                             \t{\n" +
                         "                             \t\t\"date\":\"5/11/25\",\n" +
                         "                             \t\t\"city\":\"Seattle\",\n" +
                         "                             \t\t\"center\":\"Center1\",\n" +
                         "                             \t\t\"timeSlot\":\"08:00-10:00;13:00-17:00\"\n" +
                         "                             \t},\n" +
                         "                             \t{\n" +
                         "                             \t\t\"date\":\"5/12/25\",\n" +
                         "                             \t\t\"city\":\"Seattle\",\n" +
                         "                             \t\t\"center\":\"Center2\",\n" +
                         "                             \t\t\"timeSlot\":\"08:00-10:00\"\n" +
                         "                             \t}\n" +
                         "                             ]\n" +
                         "                            If there's no routes found, returns empty json string only, no need to explain anything else." +
                         "                                                  \n" +
                         "                           The following are the availability data (json format):" +
                         "                                                  \n" +
                         "                             [\n" +
                         "                               {\n" +
                         "                                 \"city\": \"Sacramento\",\n" +
                         "                                 \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "                                 \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "                                 \"contact\": \"326-683-8045\",\n" +
                         "                                 \"date\": \"2025-02-22\",\n" +
                         "                                 \"openTimePeriod\": \"07:00-11:00; 13:00-17:00\"\n" +
                         "                               },\n" +
                         "                               {\n" +
                         "                                 \"city\": \"Sacramento\",\n" +
                         "                                 \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "                                 \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "                                 \"contact\": \"326-683-8045\",\n" +
                         "                                 \"date\": \"2025-02-23\",\n" +
                         "                                 \"openTimePeriod\": \"08:00-12:00; 09:00-13:00\"\n" +
                         "                               }\n" +
                         "                             ]";
             }
         }else{

             List<String> dateRange = params.getBeanList("homeCityDateRange",String.class);

             if(travelCtiyList.size() > 0){

                 final String[] travelCtiy = {""};

                 int i = 0;
                 int finalI = i;
                 travelCtiyList.forEach(item->{
                     if (finalI < travelCtiyList.size() - 1){
                         JSONObject jsonObject = travelCtiyList.getJSONObject(finalI);
                         travelCtiy[0] += StrUtil.format("[{}] [on {}],",jsonObject.getStr("cityName"),jsonObject.getStr("date"));
                     }
                 });
                 i++;

                 params.putOpt("visitCity", travelCtiy[0]);

                 promptTemplate ="A dialysis patient needs to drive from ["+params.getStr("homeCity")+"] to ["+params.getStr("homeCity")+"]. He plans to depart on ["+dateRange.get(0)+"] and [return] on ["+dateRange.get(0)+"]. He needs to visit "+params.getStr("visitCity")+" along the road. " +
                     "Since he needs dialysis treatment every ["+params.getStr("dialysisCycle")+"] days, he needs to find dialysis centers with available slots along the route. " +
                     "The availability data for dialysis centers is provided in the end. Please help plan his itinerary and list the driving distance for each day. He could drive at most ["+params.getStr("maximumDailyTravelDistance")+"] miles a day. \n" +
                         "return the following json format when the route is found:\n" +
                         "[\n" +
                         "\t{\n" +
                         "\t\t\"date\":\"5/11/25\",\n" +
                         "\t\t\"city\":\"Seattle\",\n" +
                         "\t\t\"center\":\"Center1\",\n" +
                         "\t\t\"timeSlot\":\"08:00-10:00;13:00-17:00\"\n" +
                         "\t},\n" +
                         "\t{\n" +
                         "\t\t\"date\":\"5/12/25\",\n" +
                         "\t\t\"city\":\"Seattle\",\n" +
                         "\t\t\"center\":\"Center2\",\n" +
                         "\t\t\"timeSlot\":\"08:00-10:00\"\n" +
                         "\t}\n" +
                         "]\n" +
                         "\n" +
                         "If there's no routes found, returns empty json string only, no need to explain anything else.\n" +
                         "\n" +
                         "The following are the availability data (json format):\n" +
                         "\n" +
                         "[\n" +
                         "  {\n" +
                         "    \"city\": \"Sacramento\",\n" +
                         "    \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "    \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "    \"contact\": \"326-683-8045\",\n" +
                         "    \"date\": \"2025-02-22\",\n" +
                         "    \"openTimePeriod\": \"07:00-11:00; 13:00-17:00\"\n" +
                         "  },\n" +
                         "  {\n" +
                         "    \"city\": \"Sacramento\",\n" +
                         "    \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "    \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "    \"contact\": \"326-683-8045\",\n" +
                         "    \"date\": \"2025-02-23\",\n" +
                         "    \"openTimePeriod\": \"08:00-12:00; 09:00-13:00\"\n" +
                         "  }\n" +
                         "]";
             }else{

                 promptTemplate = " A dialysis patient needs to drive from ["+params.getStr("homeCity")+"] to ["+params.getStr("homeCity")+"]. He plans to depart on ["+dateRange.get(0)+"] and [return] on ["+dateRange.get(1)+"].\n" +
                         "                         Since he needs dialysis treatment every ["+params.getStr("dialysisCycle")+"] days, he needs to find dialysis centers with available slots along the route.\n" +
                         "                         The availability data for dialysis centers is provided below. Please help plan his itinerary and list the driving distance for each day.\n" +
                         "                         He could drive at most ["+params.getStr("maximumDailyTravelDistance")+"] miles a day.\n" +
                         "                             return the following json format when the route is found:\n" +
                         "                             [\n" +
                         "                             \t{\n" +
                         "                             \t\t\"date\":\"5/11/25\",\n" +
                         "                             \t\t\"city\":\"Seattle\",\n" +
                         "                             \t\t\"center\":\"Center1\",\n" +
                         "                             \t\t\"timeSlot\":\"08:00-10:00;13:00-17:00\"\n" +
                         "                             \t},\n" +
                         "                             \t{\n" +
                         "                             \t\t\"date\":\"5/12/25\",\n" +
                         "                             \t\t\"city\":\"Seattle\",\n" +
                         "                             \t\t\"center\":\"Center2\",\n" +
                         "                             \t\t\"timeSlot\":\"08:00-10:00\"\n" +
                         "                             \t}\n" +
                         "                             ]\n" +
                         "                            If there's no routes found, returns empty json string only, no need to explain anything else." +
                         "                                                  \n" +
                         "                           The following are the availability data (json format):" +
                         "                                                  \n" +
                         "                             [\n" +
                         "                               {\n" +
                         "                                 \"city\": \"Sacramento\",\n" +
                         "                                 \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "                                 \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "                                 \"contact\": \"326-683-8045\",\n" +
                         "                                 \"date\": \"2025-02-22\",\n" +
                         "                                 \"openTimePeriod\": \"07:00-11:00; 13:00-17:00\"\n" +
                         "                               },\n" +
                         "                               {\n" +
                         "                                 \"city\": \"Sacramento\",\n" +
                         "                                 \"center\": \"DaVita Kidney Care - Sacramento 1\",\n" +
                         "                                 \"address\": \"9033 Main St, Sacramento, CA\",\n" +
                         "                                 \"contact\": \"326-683-8045\",\n" +
                         "                                 \"date\": \"2025-02-23\",\n" +
                         "                                 \"openTimePeriod\": \"08:00-12:00; 09:00-13:00\"\n" +
                         "                               }\n" +
                         "                             ]";
             }
         }

         String prompt = promptTemplate;

         JSONObject geminiParams = new JSONObject();

         JSONArray contents = new JSONArray();

         JSONObject parts = new JSONObject();

         JSONArray text = new JSONArray();

         JSONObject textObj = new JSONObject();
         textObj.putOpt("text",prompt);
         text.add(textObj);

         parts.putOpt("parts",text);

         contents.add(parts);

         // Construct the gemini params
         geminiParams.putOpt("contents",contents);
         // Get the result from Gemini API
         HttpResponse httpResponse = HttpRequest.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=AIzaSyBNEzEvpzg8qkm4N79PQV1CJlXmG8J_MP8")
                 .setHttpProxy("127.0.0.1",7897)
                 .header("Content-Type", "application/json")
                 .body(JSONUtil.toJsonStr(geminiParams))
                 .execute();

         String respResult =  httpResponse.body();

         String resultText = getGeminiText(respResult);
         List<JSONObject> resultList = new ArrayList<>();
         if(StrUtil.isNotBlank(resultText)){
             resultList = JSONUtil.toList(resultText,JSONObject.class);
         }
         return Result.success(resultList);
     }

     // Gemini result twreak
     private static String getGeminiText(String geminiResult){
         JSONObject result = JSONUtil.parseObj(geminiResult);
         JSONArray candidates = result.getJSONArray("candidates");
         if(!candidates.isEmpty()){
             JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
             JSONArray parts = content.getJSONArray("parts");
             JSONObject text  = parts.getJSONObject(0);
             String textStr = text.getStr("text").replaceAll("```json\n[]\n```","");
             return textStr;
         }
         return  "";
     }
}
