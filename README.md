# Getting Started

### 介绍
该项目为日志输出demo，使用了log4j2替换springboot自带的logback，因为对于海量日志输出，log4j2使用了disruptor的架构，
提高了并发输出能力。

* 依赖
```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- 排除spring-boot-starter-logging 默认是logback -->
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!--log4j2-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
<dependency>
    <groupId>com.lmax</groupId>
    <artifactId>disruptor</artifactId>
    <version>3.4.1</version>
</dependency>
```

* log4j2.xml 配置

```
 InputMDC 设置线程变量，可替换log4j2中日志输出格式的占位符
 
```



### 相关技术栈

* filebeat： 根据自定义规则监控日志文件，输出至kafka。
* kafka：通过高性能，高吞吐，海量的日志堆积能力将日志推送给logstash。
* logstash：从kafka中收取数据，并进行自定义格式化过滤（如时区转换），输出至es。
* es：分布式数据存储、搜索中间件；可设定索引模板。
* kibana：可视化数据搜索、分析工具。
* watcher：异常数据监控告警插件；可以根据es创建的索引模板，创建自定义告警规则的watcher。

### 相关配置及命令

* 创建kafka中的topic

```
kafka-topics.sh --zookeeper zookeeper:2181 --create --topic app-log-collector --partitions 1  --replication-factor 1 
kafka-topics.sh --zookeeper zookeeper:2181 --create --topic error-log-collector --partitions 1  --replication-factor 1 
```

* docs目录下为filebeat与logstash-script的相关配置

* 自定义watcher配置

```
## 创建一个每5秒轮询一次，观察error日志的watcher，并发送告警至相关API
## tips: 需要先创建 _template/error-log- 模板，再创建该watcher
PUT _xpack/watcher/watch/error_log_collector_watcher
{
  "trigger": {
    "schedule": {
      ## 创建一个每5秒轮询一次的任务触发器
      "interval": "5s"
    }
  },
  ## 查询数据的相关条件
  "input": {
    "search": {
      "request": {
        ## 监控具体索引
        "indices": ["<error-log-collector-{now+8h/d}>"],
        ## 搜索的具体条件
        "body": {
          "size": 0,
          "query": {
            "bool": {
              "must": [
                {
                  "term": {"level": "ERROR"}
                }
              ],
              "filter": {
                "range": {
                  "currentDateTime": {
                    ## 过滤出30秒以内的ERROR日志
                    "gt": "now-30s", "lt": "now"
                  }
                }
              }
            }
          }
        }
      }
    }
  },
  ## 根据input命中的结果，条件是大于0条
  "condition": {
    "compare": {
      "ctx.payload.hits.total": {
        "gt": 0
      }
    }
  },
  ## 将查出的数据进行一些排序的操作
  "transform": {
    "search": {
      "request": {
        ## 监控具体索引
        "indices": ["<error-log-collector-{now+8h/d}>"],
        ## 搜索的具体条件
        "body": {
          "size": 1,
          "query": {
            "bool": {
              "must": [
                {
                  "term": {"level": "ERROR"}
                }
              ],
              "filter": {
                "range": {
                  "currentDateTime": {
                    ## 过滤出30秒以内的ERROR日志
                    "gt": "now-30s", "lt": "now"
                  }
                }
              }
            }
          },
          "sort": [
            {
              "currentDateTime": {
                "order": "desc"
              }
            }
          ]
        }
      }
    }
  },
  ## 告警的操作，自定义告警信息发送至指定的URL
  "actions": {
    "test_error": {
      "webhook": {
        "method": "POST",
        "url": "http://192.168.31.8:8010/watcher",
        "body": "{\"title\": \"异常错误警告\", \"applicationName\": \"{{#ctx.payload.hits.hits}}{{_source.applicationName}}{{/ctx.payload.hits.hits}}\", \"level\": \"告警级别P1\", \"body\": \"{{#ctx.payload.hits.hits}}{{_source.messageInfo}}{{/ctx.payload.hits.hits}}\", \"executionTime\": \"{{#ctx.payload.hits.hits}}{{_source.currentDateTime}}{{/ctx.payload.hits.hits}}\"}"
      }
    }
  } 
}

## 创建索引模板, 成功创建模板之后，创建的所有匹配error-log-*的watcher都会引用该模板
## tips: 在创建该模板之前已存在的索引和watcher不会使用该模板；解决方案：1.删除已存在的index patterns、index、watcher；2.忽略已存在的数据，重新创建对应的index patterns、index、watcher。
PUT _template/error-log-
{
  "template": "error-log-*",
  "order": 0,
  "settings": {
    "index": {
      "refresh_interval": "5s"
    }
  },
  "mappings": {
    "_default_": {
      "dynamic_templates": [
        {
          "message_field": {
            "match_mapping_type": "string",
            "path_match": "message",
            "mapping": {
              "norms": false,
              "type": "text",
              "analyzer": "ik_max_word",
              "search_analyzer": "ik_max_word"
            }
          }
        },
        {
          "throwable_field": {
            "match_mapping_type": "string",
            "path_match": "throwable",
            "mapping": {
              "norms": false,
              "type": "text",
              "analyzer": "ik_max_word",
              "search_analyzer": "ik_max_word"
            }
          }
        },
        {
          "string_fields": {
            "match_mapping_type": "string",
            "match": "*",
            "mapping": {
              "norms": false,
              "type": "text",
              "analyzer": "ik_max_word",
              "search_analyzer": "ik_max_word",
              "fields": {
                "keyword": {
                  "type": "keyword"
                }
              }
            }
          }
        }
      ],
      "_all": {
        "enabled": false
      },
      "properties": {
        "hostName": {
          "type": "keyword"
        },
        "ip": {
          "type": "ip"
        },
        "level": {
          "type": "keyword"
        },
        "currentDateTime": {
          "type": "date"
        }
      }
    }
  }
}

## 查询前30分钟的错误日志，并以当前时间戳降序排列
GET error-log-collector-2020.04.06/_search?size=10
{
  "query": {
    "bool": {
      "must": [
        {
          "term": {"level": "ERROR"}
        }
      ],
      "filter": {
        "range": {
          "currentDateTime": {
            "gt": "now-30h", "lt": "now"
          }
        }
      }
    }
  },
  "sort": [
    {
      "currentDateTime": {
        "order": "desc"
      }
    }
  ]
}

```