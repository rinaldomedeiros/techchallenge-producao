package com.example.orderproduction;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        plugin = {"pretty", "summary"},
        glue = {"com.example.orderproduction"}
)
public class OrderStatusIT {


}