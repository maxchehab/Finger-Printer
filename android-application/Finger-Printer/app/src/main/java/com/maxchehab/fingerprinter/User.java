package com.maxchehab.fingerprinter;

public class User{
    public String username;
    public String uniqueKey;
    public String creationDate;

    public User(String username, String uniqueKey, String creationDate){
        this.username = username;
        this.uniqueKey = uniqueKey;
        this.creationDate = creationDate;
    }
}
