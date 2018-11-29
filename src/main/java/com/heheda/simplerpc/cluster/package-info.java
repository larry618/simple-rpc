package com.heheda.simplerpc.cluster;


// cluster 路由层：
// 封装多个提供者的路由及负载均衡，并桥接注册中心，
// 以 Invoker 为中心，扩展接口为 Cluster, Directory, Router, LoadBalance