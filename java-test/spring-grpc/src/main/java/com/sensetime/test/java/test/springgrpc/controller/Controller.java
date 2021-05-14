package com.sensetime.test.java.test.springgrpc.controller;

import com.sensetime.test.common.SimpleRequest;
import com.sensetime.test.common.SimpleResponse;
import com.sensetime.test.common.SpringTestGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class Controller extends SpringTestGrpc.SpringTestImplBase {
    @Override
    public void springTestMethod(SimpleRequest request, StreamObserver<SimpleResponse> responseObserver) {
        SimpleResponse response = SimpleResponse.newBuilder()
                                                .setReply(String.format("Hello %s, this is Spring-Grpc test.", request.getField()))
                                                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
