package com.ignitedatastreamer.main.metrics;

public class PerformanceMetrics {
    // Ignite Metrics
    private long igniteLoadTime;
    private long igniteReadTime;
    private long igniteUpdateTime;
    private long igniteDeleteTime;
    
    // MongoDB Metrics
    private long mongoLoadTime;
    private long mongoReadTime;
    private long mongoUpdateTime;
    private long mongoDeleteTime;

    // Getters
    public long getIgniteLoadTime() { return igniteLoadTime; }
    public long getIgniteReadTime() { return igniteReadTime; }
    public long getIgniteUpdateTime() { return igniteUpdateTime; }
    public long getIgniteDeleteTime() { return igniteDeleteTime; }
    public long getMongoLoadTime() { return mongoLoadTime; }
    public long getMongoReadTime() { return mongoReadTime; }
    public long getMongoUpdateTime() { return mongoUpdateTime; }
    public long getMongoDeleteTime() { return mongoDeleteTime; }

    // Setters
    public void setIgniteLoadTime(long igniteLoadTime) { this.igniteLoadTime = igniteLoadTime; }
    public void setIgniteReadTime(long igniteReadTime) { this.igniteReadTime = igniteReadTime; }
    public void setIgniteUpdateTime(long igniteUpdateTime) { this.igniteUpdateTime = igniteUpdateTime; }
    public void setIgniteDeleteTime(long igniteDeleteTime) { this.igniteDeleteTime = igniteDeleteTime; }
    public void setMongoLoadTime(long mongoLoadTime) { this.mongoLoadTime = mongoLoadTime; }
    public void setMongoReadTime(long mongoReadTime) { this.mongoReadTime = mongoReadTime; }
    public void setMongoUpdateTime(long mongoUpdateTime) { this.mongoUpdateTime = mongoUpdateTime; }
    public void setMongoDeleteTime(long mongoDeleteTime) { this.mongoDeleteTime = mongoDeleteTime; }
}