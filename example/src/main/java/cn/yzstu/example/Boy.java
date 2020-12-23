package cn.yzstu.example;

import cn.yzstu.core.annotation.Builder;

public class Boy {
    private int age;
    private String name;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    // use builder
    @Builder
    public void setName(String name) {
        this.name = name;
    }
}
