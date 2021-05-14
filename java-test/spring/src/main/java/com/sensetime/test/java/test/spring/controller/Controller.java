package com.sensetime.test.java.test.spring.controller;

import com.sensetime.test.common.SimpleRequest;
import com.sensetime.test.common.SimpleResponse;
import com.sensetime.test.common.SpringTestGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
public class Controller {
    private static String HOST = "localhost";
    private static int PORT = 9000;

    ManagedChannel channel = ManagedChannelBuilder.forAddress(HOST, PORT).usePlaintext().build();
    SpringTestGrpc.SpringTestBlockingStub blockingStub = SpringTestGrpc.newBlockingStub(channel);

    @RequestMapping(path = "/", method = GET)
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @GetMapping(path = "/greeting")
    public String greeting(@RequestParam(name = "username", defaultValue = "World") String username) {
        SimpleRequest request = SimpleRequest.newBuilder().setField(username).build();
        SimpleResponse response = blockingStub.springTestMethod(request);
        return response.getReply();
    }
}
