package com.example.firstapp;

import java.io.Serializable;
import java.math.BigInteger;

public class PublicInfo implements Serializable {
    public BigInteger p, g, ga, gb;

    public PublicInfo()
    {
        this.p = this.g = this.ga = this.gb = null;
    }

    public PublicInfo(BigInteger p, BigInteger g, BigInteger ga)
    {
        this.p = p;
        this.g = g;
        this.ga = ga;
        this.gb = null;
    }
}
