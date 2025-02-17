package com.route.plan.controller;

import cn.hutool.json.JSONObject;
import com.route.plan.entity.Result;
import com.route.plan.util.DataUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("renalRoute")
@CrossOrigin("*")
public class RoutePlanController {

    @GetMapping("getCityList")
    public Result<List<String>> getCityList(){
        return DataUtil.getCityList();
    }

    @GetMapping("getDataList")
    public Result<List<JSONObject>> getDataList(){
        List<JSONObject> dataList = DataUtil.getDataList();
        return Result.success(dataList);
    }

    @GetMapping("getDataListByCity")
    public Result<List<JSONObject>> getDataListByCity(String city){
        return DataUtil.getDataListByCity(city);
    }

    @PostMapping("searchPlan")
    public Result<List<JSONObject>> searchPlan(@RequestBody JSONObject params){
        return DataUtil.searchPlan(params);
    }
}
