package com.hexatomic_ring.entity_info.utils;

public class ModNumber {
    int n;
    public ModNumber(int v){
        n = v;
    }
    public void setValue(int v){
        n = v;
    }
    public int value(){
        return n;
    }
    public void add(int v){
        n += v;
    }
}
