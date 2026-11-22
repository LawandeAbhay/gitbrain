package dev.abhay.gitbrain.controllers;

import dev.abhay.gitbrain.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final String serviceName;
    private final  String servicePort;

    public HealthController( @Value("${spring.application.name}") String serviceName,
                             @Value("${server.port}") String servicePort){
        this.serviceName=serviceName;
        this.servicePort=servicePort;
    }


    @GetMapping
    public ResponseEntity<ApiResponse> getHealth(){
        String data=serviceName+" is running on port: "+servicePort;
        ApiResponse<String> apiResponse=ApiResponse.<String>builder()
                .success(true)
                .message("Service is healthy")
                .data(data)
                .build();
        return ResponseEntity.status(HttpStatus.OK)
                .body(apiResponse);
    }
}
