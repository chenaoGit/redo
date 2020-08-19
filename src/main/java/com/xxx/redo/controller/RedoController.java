package com.xxx.redo.controller;

import com.xxx.redo.service.RedoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class RedoController {

    @Resource
    private RedoService redoService;

    @GetMapping("/execute/{id}")
    public void redo(@PathVariable("id") Long id){
        // TODO 添加获取当前登录用户
        redoService.redoById(id, "admin");
    }

    @GetMapping("/cancel/{id}")
    public void cancel(@PathVariable("id") Long id){
        // TODO 添加获取当前登录用户
        redoService.cancelById(id, "admin");
    }
}
