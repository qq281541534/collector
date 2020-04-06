package com.theone.collector.controller;

import com.theone.collector.DTO.AccurateWatcherMessage;
import com.theone.collector.util.InputMDC;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class DemoController {

    @GetMapping("info")
    public String info(){
        InputMDC.putMDC();

        log.info("info 日志 {}", System.currentTimeMillis());
        return "success";
    }

    @GetMapping("err")
    public String err(){
        InputMDC.putMDC();
        try {
            throw new RuntimeException("运行时异常 " + System.currentTimeMillis());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "success";
    }

    @PostMapping("watcher")
    public String watcher(@RequestBody AccurateWatcherMessage body){
        log.info("watcher body: {}", body.toString());
        return "success";
    }
}
