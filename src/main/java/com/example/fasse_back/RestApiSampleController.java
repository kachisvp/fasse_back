package com.example.fasse_back;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestApiSampleController {

    @GetMapping("rest-api-sample")
    public String restApiSample() {
        Map<String, String> map = new HashMap<>();
        List<String> list = new ArrayList<>();
        list.add("test");
        map.put("test", "value");
        return map.toString();
    }
    
}
