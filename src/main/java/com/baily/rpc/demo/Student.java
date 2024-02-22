package com.baily.rpc.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * baily
 */
@Data
@AllArgsConstructor
@ToString
public class Student {
    private String name;
    private Integer age;
}
