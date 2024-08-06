# Bug List
This page lists all bugs found by Sonar. Up to now, we have found 37 unique bugs in MySQL, TiDB, MariaDB and SQLite, with 29 verified and 9 fixed by developers.

To alleviate the burden on developers in identifying the root cause, we have simplified some test cases for bugs discovered by Sonar before reporting them to developers. Therefore, you will find the test cases are not always the oracle form of Sonar.

## MySQL

* #1 [http://bugs.mysql.com/114196](https://bugs.mysql.com/bug.php?id=114196)

  **Status**: fixed

  **Version**: 8.0.35

  **Test case**

  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 DECIMAL);
  REPLACE INTO t0(c0) VALUES(0.1);
  
  SELECT t0.c0 FROM t0 WHERE t0.c0 <= - 0.8; -- expected:{} actual:{0}
  ```

* #2[ http://bugs.mysql.com/113586](https://bugs.mysql.com/bug.php?id=113586)

  **Status**: Verified

  **Version**: 8.0

  **Test case**

  ```sql
  CREATE TABLE t0(c0 DECIMAL) ;
  REPLACE INTO t0(c0) VALUES("512");
  
  SELECT f1 FROM (
  SELECT t0.c0 >> IFNULL("\r8*&t", NULL) AS f1 FROM t0
  ) AS t WHERE f1!=123;
  +------+
  | f1   |
  +------+
  |    2 |
  +------+
  1 row in set (0.00 sec)
 
  SELECT f1 FROM (
  SELECT t0.c0 >> (IFNULL("\r8*&t", NULL)) AS f1,
  ((t0.c0 >> IFNULL("\r8*&t", NULL)) != 123) IS TRUE AS flag FROM t0
  ) AS t WHERE flag=1;
  +------+
  | f1   |
  +------+
  |  512 |
  +------+
  1 row in set (0.00 sec)

  ```
  
* #3[ http://bugs.mysql.com/114149](https://bugs.mysql.com/bug.php?id=114149)

  **Status**: Verified

  **Version**: 8.0

  **Test case**

  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 SMALLINT(157)); 
  INSERT INTO t0(c0) VALUES(1); 
  
  SELECT f1 FROM (SELECT (t0.c0 - SUBDATE('2022-07-06' ,INTERVAL 47 MINUTE)) AS f1 FROM t0) AS t; -- expected:{-2021} actual:{-20220705231299}
  ```


* #4[ http://bugs.mysql.com/114224](https://bugs.mysql.com/bug.php?id=114224)

  **Status**: Verified
 
  **Version**: 8.0
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c0 TINYINT);
  CREATE INDEX i0 ON t0(c0 DESC); 
  INSERT IGNORE INTO t0(c0) VALUES("c");
  
  SELECT t0.c0 FROM t0 WHERE t0.c0=ABS(0.1); -- expected:{} actual:{0}
  ```

* #5[ http://bugs.mysql.com/114282](https://bugs.mysql.com/bug.php?id=114282)

  **Status**: Verified
 
  **Version**: 8.0.35
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c0 FLOAT) ;
  INSERT INTO t0(c0) VALUES(NULL);
  
  SELECT COALESCE(t0.c0, 400145287) AS f1 FROM t0 GROUP BY c0 HAVING (f1) & (0.6979778231950815);
  Empty set (0.01 sec)
  
  SELECT f1 FROM (SELECT COALESCE(t0.c0, 400145287) AS f1, (COALESCE(t0.c0, 400145287) & (0.6979778231950815)) IS TRUE AS flag FROM t0 GROUP BY c0 HAVING flag=1) as t;
  +-----------+
  | f1        |
  +-----------+
  | 400145000 |
  +-----------+
  ```

* #6[ http://bugs.mysql.com/114384](https://bugs.mysql.com/bug.php?id=114384)

  **Status**: Verified
 
  **Version**: 8.0.15,8.0.36
 
  **Test case**
 
  ```sql
  CREATE TABLE IF NOT EXISTS t1(c0 SMALLINT) ;
  INSERT INTO t1(c0) VALUES(0.49);
  CREATE INDEX i0 ON t1(c0);
  
  SELECT t1.c0 FROM t1 WHERE t1.c0 IN (LOG(0.6261534882548163));
  +------+
  | c0   |
  +------+
  |    0 |
  +------+
  1 row in set (0.00 sec)
  
  SELECT c0 FROM (SELECT t1.c0, (t1.c0 IN (LOG(0.6261534882548163))) IS TRUE AS flag FROM t1) as t WHERE flag=1;
  Empty set (0.00 sec)
  ```

* #7[ http://bugs.mysql.com/114383](https://bugs.mysql.com/bug.php?id=114383)

  **Status**: Verified
 
  **Version**: 8.0.23,8.0.35
 
  **Test case**
 
  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 INT) ;
  REPLACE DELAYED INTO t0(c0) VALUES(-1);
  
  SELECT ALL f1 FROM (SELECT ALL (MAX(DATE('2024-01-01 ')) OVER (PARTITION BY t0.c0)) AS f1 FROM t0) as t WHERE (f1) > (1.105003755E9);
  +------------+
  | f1         |
  +------------+
  | 2024-01-01 |
  +------------+
  1 row in set, 2 warnings (0.00 sec)
  
  SELECT f1 FROM (SELECT (MAX(DATE('2024-01-01 ')) OVER (PARTITION BY t0.c0)) AS f1, ((MAX(DATE('2024-01-01 ')) OVER (PARTITION BY t0.c0)) > (1.105003755E9)) IS TRUE AS flag FROM t0) as t WHERE flag=1;
  Empty set, 2 warnings (0.00 sec)
  ```

* #8[ http://bugs.mysql.com/114382](https://bugs.mysql.com/bug.php?id=114382)

  **Status**: Verified
 
  **Version**: 8.0.11,8.0.36
 
  **Test case**
 
  ```sql
  CREATE TABLE IF NOT EXISTS t1(c0 TINYINT(199)) ;
  INSERT LOW_PRIORITY IGNORE INTO t1(c0) VALUES (1950654919);

  SELECT f1 FROM (SELECT (LEAST(CURDATE(), (- (t1.c0)), (~ (t1.c0)),  EXISTS (SELECT 1))) AS f1 FROM t1) as t WHERE f1;
  +------+
  | f1   |
  +------+
  | -127 |
  +------+
  1 row in set, 3 warnings (0.00 sec)
  
  SELECT f1 FROM (SELECT (LEAST(CURDATE(), (- (t1.c0)),  EXISTS (SELECT 1))) AS f1, (LEAST(CURDATE(), (- (t1.c0)),   EXISTS (SELECT 1)) ) IS TRUE AS flag FROM t1) as t WHERE flag=1;
  Empty set, 6 warnings (0.00 sec)
  ```

* #9[ http://bugs.mysql.com/114381](https://bugs.mysql.com/bug.php?id=114381)

  **Status**: Verified
 
  **Version**: 8.0.19,8.0.35
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c0 DOUBLE) ;
  CREATE TABLE t1 LIKE t0;
  INSERT IGNORE INTO t1(c0) VALUES(1);
  INSERT INTO t0(c0) VALUES(0.1);

  SELECT t1.c0 AS _c0 FROM t1, t0 WHERE (( EXISTS (SELECT 1 )) < ((NOT t0.c0))) IN (((t1.c0) XOR ("")) / (TIMEDIFF('3939-09-13 16:49:10.309835', '4722-09-08 23:55:52.675528')));
  +------+
  | _c0  |
  +------+
  |    1 |
  +------+
  1 row in set, 1 warning (0.00 sec)

  SELECT _c0 FROM (SELECT t1.c0 AS _c0, ((( EXISTS (SELECT 1 )) < ((NOT t0.c0))) IN (((t1.c0) XOR ("")) / (TIMEDIFF('3939-09-13 16:49:10.309835', '4722-09-08 23:55:52.675528')))) IS TRUE AS flag FROM t1, t0) as t     WHERE flag=1;
  Empty set, 1 warning (0.00 sec)
  ```

* #10[ http://bugs.mysql.com/114380](https://bugs.mysql.com/bug.php?id=114380)

  **Status**: Verified
 
  **Version**: 8.0.13,8.0.36
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c1 FLOAT) ;
  INSERT INTO t0(c1) VALUES(0.1);
  
  SELECT f1 FROM (SELECT (LEAST('-2',  EXISTS (SELECT 1  ), CAST(t0.c1 AS SIGNED), TIMESTAMP('2024-03-17'))) AS f1 FROM t0) as t WHERE f1;
  +------+
  | f1   |
  +------+
  | -2   |
  +------+
  1 row in set, 3 warnings (0.00 sec)
  
  SELECT f1 FROM (SELECT (LEAST('-2',  EXISTS (SELECT 1  ), CAST(t0.c1 AS SIGNED), TIMESTAMP('2024-03-17'))) AS f1, (LEAST('-2',  EXISTS (SELECT 1  ), CAST(t0.c1 AS SIGNED), TIMESTAMP('2024-03-17'))) IS TRUE AS    
  flag FROM t0) as t WHERE flag=1;
  Empty set, 6 warnings (0.00 sec)
  ```

* #11[ http://bugs.mysql.com/114379](https://bugs.mysql.com/bug.php?id=114379)

  **Status**: Verified
 
  **Version**: 8.0.21,8.0.35
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c0 FLOAT) ;
  CREATE TABLE t1(c0 DECIMAL);
  INSERT INTO t1(c0) VALUES(1);
  CREATE INDEX i1 ON t0((t0.c0 IS NOT TRUE), ((t0.c0 IS NOT FALSE) & (NULL XOR t0.c0)));

  SELECT t1.c0 AS _c0 FROM t1 LEFT OUTER JOIN t0 ON 1 WHERE t0.c0 IS NOT TRUE;
  +------+
  | _c0  |
  +------+
  |    1 |
  +------+
  1 row in set (0.00 sec)
  
  SELECT _c0 FROM (SELECT  t1.c0 AS _c0, (t0.c0 IS NOT TRUE) IS TRUE AS flag FROM t1 LEFT OUTER JOIN t0 ON 1 ) as t WHERE flag=1;
  Empty set (0.00 sec)
  ```

* #12[ http://bugs.mysql.com/114378](https://bugs.mysql.com/bug.php?id=114378)

  **Status**: Verified
 
  **Version**: 8.0
 
  **Test case**
 
  ```sql
  CREATE TABLE t0(c0 FLOAT);
  INSERT INTO t0(c0) VALUES(-1);
  
  SELECT f1 FROM (SELECT ((t0.c0) ^ (CONVERT_TZ('2001-10-22 16:13:59.9', 'GMT','MET'))) AS f1 FROM t0) as t WHERE (f1) > (1); 
  +----------------------+
  | f1                   |
  +----------------------+
  | 18446724062687370255 |
  +----------------------+
  1 row in set (0.00 sec)
  
  SELECT ALL f1 FROM (SELECT ((t0.c0) ^ (CONVERT_TZ('2001-10-22 16:13:59.9', 'GMT','MET'))) AS f1, (((t0.c0) ^ (CONVERT_TZ('2001-10-22 16:13:59.9', 'GMT','MET'))) > (1)) IS TRUE AS flag FROM t0) as t WHERE flag=1;
  +----------------------+
  | f1                   |
  +----------------------+
  | 18446724062687370215 |
  +----------------------+
  1 row in set (0.00 sec)
  ```

* #13[ http://bugs.mysql.com/113578](https://bugs.mysql.com/bug.php?id=113578)

  **Status**: Verified
 
  **Version**: 8.0
 
  **Test case**
 
  ```sql
  CREATE TABLE IF NOT EXISTS t1(c0 int) ;
  REPLACE INTO t1(c0) VALUES(1),(2);
  
  SELECT AVG(-1.7E308) AS f1 FROM t1 HAVING f1;
  +------+
  | f1   |
  +------+
  |    0 |
  +------+
  1 row in set (0.00 sec)
  
  SELECT f1 FROM (SELECT AVG(-1.7E308) AS f1, AVG(-1.7E308) IS TRUE AS flag FROM t1 HAVING flag=1) AS tmp_t;
  +-------------------------+
  | f1                      |
  +-------------------------+
  | -1.7976931348623157e308 |
  +-------------------------+
  1 row in set (0.00 sec)
  ```



## TiDB

* #1 [https://github.com/pingcap/tidb/issues/51842](https://github.com/pingcap/tidb/issues/51842)

  **Status**: Verified

  **Version**: v6.4.0, v7.6.0

  **Test case**

  ```sql
  CREATE TABLE t0(c0 DOUBLE);
  REPLACE INTO t0(c0) VALUES (0.40194983109852933);
  CREATE VIEW v0(c0) AS SELECT CAST(')' AS TIME) FROM t0 WHERE '0.030417148673465677';
  
  SELECT f1 FROM (SELECT NULLIF(v0.c0, 1371581446) AS f1 FROM v0, t0) AS t WHERE f1 <=> 1292367147;
  +------+
  | f1   |
  +------+
  | NULL |
  +------+
  1 row in set, 3 warnings (0.01 sec)
  
  SELECT f1 FROM (SELECT NULLIF(v0.c0, 1371581446) AS f1, (NULLIF(v0.c0, 1371581446) <=> 1292367147 ) IS TRUE AS flag FROM v0, t0) AS t WHERE flag=1;
  Empty set, 3 warnings (0.00 sec)
  ```
  
* #2 [https://github.com/pingcap/tidb/issues/51096](https://github.com/pingcap/tidb/issues/51096)

  **Status**: Verified

  **Version**: v7.5.0

  **Test case**

  ```sql
  CREATE TABLE t0(c0 NUMERIC UNSIGNED , c1 DOUBLE, c2 BOOL );
  REPLACE INTO t0 VALUES (1726229803, 0.15695553372105964, false);
  
  SELECT f1 FROM (SELECT (((t0.c0)/(1475275145))) AS f1 FROM t0) AS t WHERE ((f1)+('?'));
  +----------------------------------+
  | f1                               |
  +----------------------------------+
  | 1.170107019000000000000000000000 |
  +----------------------------------+
  1 row in set, 1 warning (0.00 sec)
  
  SELECT f1 FROM (SELECT (((t0.c0)/(1475275145))) AS f1, (((((t0.c0)/(1475275145)))+('?'))) IS TRUE AS flag FROM t0) AS t WHERE flag=1;
  +--------+
  | f1     |
  +--------+
  | 1.1701 |
  +--------+
  1 row in set, 1 warning (0.00 sec)
  ```
* #3 [https://github.com/pingcap/tidb/issues/51840](https://github.com/pingcap/tidb/issues/51840)

  **Status**: Verified

  **Version**: v5.0.1,v7.6.0 

  **Test case**

  ```sql
  CREATE TABLE t0(c0 BIGINT , c1 BLOB(301) , c2 BOOL);
  INSERT INTO t0 VALUES (1, 1, 1);
  
  SELECT f1 FROM (SELECT (CONNECTION_ID()) AS f1 FROM t0) AS t WHERE ((f1)>=(-1.487944961E9));  --query1
  Empty set (0.00 sec)
  
  SELECT f1 FROM (SELECT (CONNECTION_ID()) AS f1, (((CONNECTION_ID())>=(-1.487944961E9))) IS TRUE AS flag FROM t0) AS t  WHERE flag=1;  --query2
  +---------+
  | f1      |
  +---------+
  | 2097158 |
  +---------+
  1 row in set (0.00 sec)
  ```

* #4 [https://github.com/pingcap/tidb/issues/51361](https://github.com/pingcap/tidb/issues/51361)

  **Status**: Verified

  **Version**: v7.6.0 

  **Test case**

  ```sql
  CREATE TABLE t0(c0 FLOAT);
  CREATE VIEW v0(c0) AS SELECT t0.c0 FROM t0;
  INSERT INTO t0(c0) VALUES (NULL);
  
  SELECT t0.c0 FROM v0 LEFT JOIN t0 ON 1 WHERE (TIMEDIFF( '2003-07-13', '2007-06-25') AND true);
  +------+
  | c0   |
  +------+
  | NULL |
  +------+
  1 row in set, 2 warnings (0.01 sec)
  
  SELECT c0 FROM (SELECT t0.c0, (TIMEDIFF('2003-07-13', '2007-06-25') AND true) IS TRUE AS flag FROM v0 LEFT JOIN t0 ON 1) AS t WHERE flag=1;
  +------------+
  | c0         |
  +------------+
  | 1.4013e-45 |
  +------------+
  1 row in set, 4 warnings (0.01 sec)
  ```

* #5 [https://github.com/pingcap/tidb/issues/51359](https://github.com/pingcap/tidb/issues/51359)

  **Status**: Verified

  **Version**: v7.6.0 

  **Test case**

  ```sql
  CREATE TABLE t0(c0 BOOL);
  REPLACE INTO t0(c0) VALUES (false), (true);
  CREATE VIEW v0(c0) AS SELECT (REGEXP_LIKE(t0.c0, t0.c0)) FROM t0 WHERE t0.c0 GROUP BY t0.c0 HAVING 1;
 
  SELECT t0.c0 FROM v0, t0 WHERE (SUBTIME('2001-11-28 06', '252 10') OR ('' IS NOT NULL));
  +------+
  | c0   |
  +------+
  |    0 |
  |    1 |
  +------+
  2 rows in set, 3 warnings (0.00 sec)
  
  SELECT t0.c0 FROM v0, t0 WHERE (SUBTIME('2001-11-28 06', '252 10') OR ('' IS NOT NULL)) AND v0.c0;
  +------+
  | c0   |
  +------+
  |    1 |
  |    1 |
  +------+
  2 rows in set, 3 warnings (0.00 sec)
  ```

* #6 [https://github.com/pingcap/tidb/issues/51350](https://github.com/pingcap/tidb/issues/51350)

  **Status**: Verified

  **Version**: v7.6.0 

  **Test case**

  ```sql
  CREATE TABLE t1(c0 FLOAT,c1 FLOAT);
  INSERT INTO t1 VALUES (0, 1.1);
  CREATE VIEW v0(c1, c2) AS SELECT t1.c0, CAST(t1.c1 AS DECIMAL) FROM t1;
  
  SELECT v0.c2 FROM v0;
  +------+
  | c2   |
  +------+
  |    1 |
  +------+
  1 row in set, 1 warning (0.00 sec)
  
  SELECT v0.c2 FROM v0 WHERE (CASE v0.c2 WHEN v0.c1 THEN 1 ELSE 1 END );
  +----------------------------------+
  | c2                               |
  +----------------------------------+
  | 1.100000023841858000000000000000 |
  +----------------------------------+
  1 row in set (0.00 sec)
  ```

* #7 [https://github.com/pingcap/tidb/issues/51292](https://github.com/pingcap/tidb/issues/51292)

  **Status**: fixed

  **Version**: v7.5.0 

  **Test case**

  ```sql
  CREATE TABLE t0(c0 DECIMAL ZEROFILL UNIQUE , c1 BOOL ZEROFILL AS (-1));
  INSERT IGNORE  INTO t0(c0) VALUES (NULL);
  CREATE INDEX i0 ON t0(c1 ASC, c0 DESC);
  
  SELECT t0.c1 FROM t0;
  +------+
  | c1   |
  +------+
  |  255 |
  +------+
  1 row in set (0.00 sec)
  
  SELECT t0.c1 FROM t0 WHERE (t0.c0 IS NULL);
  +------+
  | c1   |
  +------+
  |    0 |
  +------+
  1 row in set, 1 warning (0.00 sec)
  ```

* #8 [https://github.com/pingcap/tidb/issues/51290](https://github.com/pingcap/tidb/issues/51290)

  **Status**: Fixed

  **Version**: v7.5.0 

  **Test case**

  ```sql
  CREATE TABLE t0(c0 TINYINT);
  CREATE VIEW v0(c0) AS SELECT t0.c0 FROM t0 GROUP BY '1';
  INSERT IGNORE INTO t0(c0) VALUES (-1);
    
  SELECT t0.c0 AS f1, (TIMEDIFF('2001-11-25', '2008-03-06') AND 1 ) IS TRUE AS flag FROM t0 INNER JOIN v0;
  +------+------+
  | f1   | flag |
  +------+------+
  |   -1 |    1 |
  +------+------+
  1 row in set, 2 warnings (0.01 sec)
    
  SELECT f1 FROM (SELECT t0.c0 AS f1, (TIMEDIFF('2001-11-25', '2008-03-06') AND 1 ) IS TRUE AS flag FROM t0 INNER JOIN v0) AS t WHERE flag=1;
  +------+
  | f1   |
  +------+
  |    1 |
  +------+
  1 row in set, 4 warnings (0.00 sec)
  ```

* #9 [https://github.com/pingcap/tidb/issues/51841](https://github.com/pingcap/tidb/issues/51841)

  **Status**: Verified

  **Version**: v5.3.2, v7.6.0

  **Test case**

  ```sql
  CREATE TABLE t0(c0 TEXT(119));
  INSERT INTO t0 VALUES ('?');
  CREATE VIEW v4(c0) AS SELECT CAST(t0.c0 AS DECIMAL) FROM t0;
  
  SELECT v4.c0 AS _c0 FROM v4 WHERE (v4.c0 = COALESCE(-164345996, v4.c0, CASE v4.c0 WHEN -546905304 THEN 'e' ELSE 1760598647 END)) LIKE v4.c0;  --query1
  Empty set, 3 warnings (0.01 sec)
  
  SELECT _c0 FROM (SELECT v4.c0 AS _c0, (v4.c0 = COALESCE(-164345996, v4.c0, CASE v4.c0 WHEN -546905304 THEN 'e' ELSE 1760598647 END)) LIKE v4.c0 AS flag FROM v4) AS t WHERE flag = 1; --query2
  +------+
  | _c0  |
  +------+
  |    0 |
  +------+
  1 row in set, 3 warnings (0.00 sec)
  ```

## MariaDB

* #1 [https://jira.mariadb.org/browse/MDEV-33307](https://jira.mariadb.org/browse/MDEV-33307)

  **Status**: Verified

  **Version**: 10.4,11.4.1

  **Test case**

  ```sql
  CREATE TABLE t0(c0 DECIMAL NULL, c1 DECIMAL);
  INSERT IGNORE INTO t0(c0, c1) VALUES('-1', 1); 
      
  SELECT f1 FROM (SELECT (t0.c0 - ADDDATE('2024-01-01', 1)) AS f1 FROM t0) as t WHERE f1;
  +-------+
  | f1    |
  +-------+
  | -2025 |
  +-------+
    
  SELECT f1 FROM (SELECT (t0.c0 - ADDDATE('2024-01-01', 1)) AS f1, (t0.c0 - ADDDATE('2024-01-01', 1)) IS TRUE AS flag FROM t0) as t WHERE flag=1;
  +-----------+
  | f1        |
  +-----------+
  | -20240103 |
  +-----------+
  ```

* #2 [https://jira.mariadb.org/browse/MDEV-33708](https://jira.mariadb.org/browse/MDEV-33708)

  **Status**: open

  **Version**: 10.9,11.4.1

  **Test case**

  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 BIGINT);
  INSERT INTO t0 VALUES (1567981295);
  
  mysql> SELECT t0.c0 FROM t0 WHERE COS(t0.c0);
  Empty set (0.00 sec)
   
  mysql> SELECT c0 FROM (SELECT t0.c0, COS(t0.c0) IS TRUE AS flag FROM t0) AS t WHERE flag=1;
  +------------+
  | c0         |
  +------------+
  | 1567981295 |
  +------------+
  1 row in set (0.01 sec)
  ```

* #3 [https://jira.mariadb.org/browse/MDEV-33707](https://jira.mariadb.org/browse/MDEV-33707)

  **Status**: open

  **Version**: 10.4.21, 11.4.1

  **Test case**

  ```sql
  CREATE TABLE t0(c0 CHAR(100), c1 BIGINT, c2 REAL UNIQUE, PRIMARY KEY(c2, c1, c0));
  CREATE TABLE t1 LIKE t0;
  INSERT INTO t0 VALUES ('1', 95607293, -1);
  
  mysql> SELECT f1 FROM (SELECT (t0.c1 - (t1.c0 IS TRUE)) AS f1 FROM t1 RIGHT OUTER JOIN t0 ON NULL) AS t WHERE f1 AND 1;
  Empty set (0.00 sec)
   
  mysql> SELECT f1 FROM (SELECT (t0.c1 - (t1.c0 IS TRUE)) AS f1, ((t0.c1 - (t1.c0 IS TRUE)) AND 1) IS TRUE AS flag FROM t1 RIGHT OUTER JOIN t0 ON NULL) AS t WHERE flag=1;
  +----------+
  | f1       |
  +----------+
  | 95607293 |
  +----------+
  1 row in set (0.00 sec)
  ```
  
* #4 [https://jira.mariadb.org/browse/MDEV-33705](https://jira.mariadb.org/browse/MDEV-33705)

  **Status**: open

  **Version**: 10.0.15, 11.4.1

  **Test case**

  ```sql
  CREATE TABLE t0(c0 BIGINT);
  CREATE OR REPLACE TABLE t1(c0 VARCHAR(100));
  INSERT INTO t0 VALUES (-1838284247);
  INSERT INTO t1 VALUES ('1');
    
  mysql> SELECT (t1.c0) AS _c0 FROM t0, t1 WHERE COT(t0.c0);
  Empty set (0.00 sec)

  mysql> SELECT _c0 FROM (SELECT (t1.c0) AS _c0, (COT(t0.c0)) IS TRUE AS flag FROM t0, t1) AS t WHERE flag=1;
  +------+
  | _c0  |
  +------+
  | 1    |
  +------+
  1 row in set (0.00 sec)
  ```

* #5 [https://jira.mariadb.org/browse/MDEV-33345](https://jira.mariadb.org/browse/MDEV-33345)

  **Status**: open

  **Version**: 11.4.1

  **Test case**

  ```sql
  CREATE TABLE t0(c0 CHAR(100)  PRIMARY KEY NOT NULL);
  INSERT INTO t0 VALUES (1);
    
  SELECT f1 FROM (SELECT (t0.c0 % (-1| 1 )) AS f1 FROM t0) AS t WHERE (f1+1); 
  +-------+
  | f1    |
  +-------+
  | 0     |
  +-------+
  1 row in set (0.00 sec)
  
  SELECT f1 FROM (SELECT (t0.c0 % (-1| 1 )) AS f1, ((t0.c0 % (-1| 1 ))+1) IS TRUE AS flag FROM t0) AS t WHERE flag=1; 
  +-------+
  | f1    |
  +-------+
  | 1     |
  +-------+
  1 row in set (0.00 sec)
  ```

* #6 [https://jira.mariadb.org/browse/MDEV-33337](https://jira.mariadb.org/browse/MDEV-33337)

  **Status**: open

  **Version**: 11.4.1

  **Test case**

  ```sql
  CREATE TABLE t0(c0 INT) ;
  INSERT INTO t0(c0) VALUES(0.6799213532830038);
    
  SELECT f1 FROM (SELECT (t0.c0) / (PERIOD_ADD(196802,2)) AS f1 FROM t0) AS t WHERE f1;  
  Empty set (0.00 sec)
  
  SELECT f1 FROM (SELECT (t0.c0) / (PERIOD_ADD(196802,2)) AS f1, ((t0.c0) / (PERIOD_ADD(196802,2))) IS TRUE AS flag FROM t0) AS t WHERE flag=1;
  +-----------+
  | f1        |
  +-----------+
  | 0.0000    |
  +-----------+
  1 row in set (0.00 sec)
  ```

* #7 [https://jira.mariadb.org/browse/MDEV-33336](https://jira.mariadb.org/browse/MDEV-33336)

  **Status**: open

  **Version**: 11.4.1

  **Test case**

  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 DOUBLE); 
  INSERT INTO t0(c0) VALUES(1); 
      
  SELECT f1 FROM (SELECT ((t0.c0) - (TIMESTAMPADD(MICROSECOND, 4 , '2023-04-18'))) AS f1 FROM t0) as t WHERE f1;
  +-------+
  | f1    |
  +-------+
  | -2022 |
  +-------+
  1 row in set (0.00 sec)
  
  SELECT f1 FROM (SELECT ((t0.c0) - (TIMESTAMPADD(MICROSECOND, 4 , '2023-04-18'))) AS f1, ((t0.c0) - (TIMESTAMPADD(MICROSECOND, 4 , '2023-04-18'))) IS TRUE AS flag FROM t0) as t WHERE flag=1;    
  +------------------+
  | f1               |
  +------------------+
  | -20230417999999  |
  +------------------+
  1 row in set (0.00 sec)
  ```

* #8 [https://jira.mariadb.org/browse/MDEV-33335](https://jira.mariadb.org/browse/MDEV-33335)

  **Status**: open

  **Version**: 11.4.1

  **Test case**

  ```sql
  CREATE TABLE IF NOT EXISTS t0(c0 SMALLINT); 
  INSERT INTO t0(c0) VALUES(1);  
      
  SELECT DISTINCT (t0.c0) - (SUBDATE('2023-01-01' ,INTERVAL 1 MINUTE)) AS f1 FROM t0 HAVING f1;   
  +-------+
  | f1    |
  +-------+
  | -2021 |
  +-------+
    
  SELECT f1 FROM (SELECT DISTINCT (t0.c0) - (SUBDATE('2023-01-01' ,INTERVAL 1 MINUTE)) AS f1, ((t0.c0) - (SUBDATE('2023-01-01' ,INTERVAL 1 MINUTE))) IS TRUE AS flag FROM t0 HAVING flag=1) as tmp_t;
  +-----------------+
  | f1              |
  +-----------------+
  | -20221231235899 |
  +-----------------+
  ```

* #9 [https://jira.mariadb.org/browse/MDEV-33555](https://jira.mariadb.org/browse/MDEV-33555)

  **Status**: open

  **Version**: 10.9.1,11.4.1

  **Test case**

  ```sql
  CREATE TABLE t1(c0 VARCHAR(100));
  INSERT INTO t1 VALUES ('1');
      
  SELECT f1 FROM (SELECT (t1.c0 - (-1^1)) AS f1 FROM t1) AS t;
  +------------------------+
  | f1                     |
  +------------------------+
  | -1.8446744073709552e19 |
  +------------------------+
  1 row in set (0.00 sec)
    
  SELECT f1 FROM (SELECT (t1.c0 - (-1^1)) AS f1 FROM t1) AS t WHERE f1;
  +------+
  | f1   |
  +------+
  |    3 |
  +------+
  1 row in set (0.00 sec)
  ```


## SQLite

* #1 [https://sqlite.org/forum/forumpost/9dcb5f4c4a](https://sqlite.org/forum/forumpost/9dcb5f4c4a)

  **Status**: fixed

  **Version**: 3.43.0

  **Test case**

  ```sql
  CREATE TABLE t0 (c0 DOUBLE );
  INSERT INTO t0(c0) VALUES (1),(2),(3);

  SELECT SUM(1.7976931348623157E308) as aggr FROM t0 WHERE c0 > 1; -- {NULL}

  SELECT aggr FROM (SELECT SUM(1.7976931348623157E308) as aggr, (c0 > 1) is true as flag FROM t0) WHERE flag=1; -- {}
  ```

* #2 [https://sqlite.org/forum/info/1bb055be177e4e4c](https://sqlite.org/forum/info/1bb055be177e4e4c)

  **Status**: fixed

  **Version**: 3.46.0

  **Test case**

  ```sql
  CREATE VIRTUAL TABLE rt1 USING rtree_i32(c0, c1, c2, +c3 INT );
  INSERT INTO rt1(c0, c2, c3) VALUES ('9223372036854775807', '1840618558', 0.35548821863495284);
  CREATE VIEW v0(c4) AS SELECT CAST(COALESCE(DISTINCT c0,c0) AS BLOB) FROM rt1;
  
  SELECT (c0==CAST(c4 AS REAL)) AS f1 FROM rt1, v0 WHERE f1; -- {0}
  
  SELECT f1 FROM (SELECT (c0==CAST(c4 AS REAL)) AS f1, (c0==CAST(c4 AS REAL)) IS TRUE AS flag FROM rt1, v0 WHERE flag=1); -- {}
  ```

* #3 [https://sqlite.org/forum/forumpost/40da29c1f6](https://sqlite.org/forum/forumpost/40da29c1f6)

  **Status**: fixed

  **Version**: 3.46.0

  **Test case**

  ```sql
  CREATE VIRTUAL TABLE vt0 USING fts5(c0, c1 UNINDEXED);
  CREATE TABLE t1 (c2 float);
  CREATE INDEX i0 ON t1(NULL);
  INSERT INTO t1(c2) VALUES (0.2);
  CREATE VIEW v0(c3) AS SELECT DISTINCT c2 FROM t1;

  SELECT c2 FROM v0 FULL OUTER JOIN vt0 ON ((UPPER( c3))<(NULL)) LEFT OUTER JOIN t1 ON 1; -- {0.2}
  
  SELECT c2 FROM v0 FULL OUTER JOIN vt0 ON ((UPPER( c3))<(NULL)) LEFT OUTER JOIN t1 ON 1 WHERE c2/0.1; -- {}
  ```

* #4 [https://sqlite.org/forum/forumpost/8a6e383777](https://sqlite.org/forum/forumpost/8a6e383777)

  **Status**: fixed

  **Version**: 3.46.0

  **Test case**

  ```sql
  CREATE TABLE v0 ( c1 INTEGER PRIMARY KEY, c2 TEXT);
  CREATE VIEW v5 AS SELECT c1, COUNT ( * ) AS y, sum ( c2 ) OVER ( PARTITION BY c1) FROM v0;

  SELECT c1 from v5; -- {NULL}

  SELECT c1 FROM v5 WHERE c1 IS NULL; -- {}
  ```
  
* #5 [https://sqlite.org/forum/forumpost/e28e4a3b1a](https://sqlite.org/forum/forumpost/e28e4a3b1a)

  **Status**: fixed

  **Version**: 3.35.0

  **Test case**

  ```sql
  CREATE TABLE v0 ( c1 INTEGER PRIMARY KEY ON CONFLICT REPLACE, c2 UNIQUE );  
  INSERT INTO v0 VALUES ( 0, 33 ), ( 11, 22 );
  REPLACE INTO v0 VALUES ( 0, 11 ) ON CONFLICT ( c2 ) DO UPDATE SET c1 = c2, c2 = c2 ON CONFLICT ( c2 ) DO UPDATE SET c1 = c1, c2 = c1;  
 
  SELECT count(*) FROM v0 WHERE c2 > 8;
  -- expected: {2} actual: {3}
  ```

* #6 [https://sqlite.org/forum/info/d619189e239fc2b9](https://sqlite.org/forum/info/d619189e239fc2b9)

  **Status**: fixed

  **Version**: 3.46.0

  **Test case**

  ```sql
  CREATE TABLE rt0 (c0 INTEGER, c1 INTEGER, c2 INTEGER, c3 INTEGER, c4 INTEGER);
  CREATE TABLE rt3 (c0 INTEGER, c1 INTEGER, c2 INTEGER,c3 INTEGER);
  INSERT OR IGNORE INTO rt0(c3, c1) VALUES (x'', '1'), ('-1', -1e500), (1, x'');
  CREATE VIEW v6(c0, c1, c2) AS SELECT 0, 0, 0;
  
  SELECT COUNT(*) FROM rt0 LEFT OUTER JOIN rt3 ON NULL RIGHT OUTER JOIN v6 ON ((CASE v6.c0 WHEN rt0.c4 THEN rt3.c3 END) NOT BETWEEN (rt0.c4) AND (NULL)) WHERE (rt0.c1);
  -- expected: {0} actual: {2}

  ```


