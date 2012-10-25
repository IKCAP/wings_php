-- MySQL dump 10.11
-- ---------------------
-- Server version	5.0.27

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `seeds`
--

DROP TABLE IF EXISTS `seeds`;
CREATE TABLE `seeds` (
  `seed_name` varchar(64) NOT NULL,
  `seed_id` varchar(64) NOT NULL,
  `user_id` varchar(64) default NULL,
  `ip_address` varchar(16) default NULL,
  `host_name` varchar(64) default NULL,
  `timestamp` varchar(64) default NULL,
  `seed` text NOT NULL,
  PRIMARY KEY  (`seed_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `atomic_seeds`
--

DROP TABLE IF EXISTS `atomic_seeds`;
CREATE TABLE `atomic_seeds` (
  `seed_id` varchar(64) NOT NULL default '',
  `atomic_seed_id` varchar(64) NOT NULL default '',
  `seed` text,
  PRIMARY KEY  (`atomic_seed_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `data_location`
--

DROP TABLE IF EXISTS `data_location`;
CREATE TABLE `data_location` (
  `data_object_id` varchar(64) NOT NULL,
  `location` varchar(2000) NOT NULL 
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `data_metrics`
--

DROP TABLE IF EXISTS `data_metrics`;
CREATE TABLE `data_metrics` (
  `data_object_id` varchar(64) NOT NULL,
  `metrics` longblob NOT NULL default '',
  PRIMARY KEY  (`data_object_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `dax_component_argument_maps`
--

DROP TABLE IF EXISTS `dax_component_argument_maps`;
CREATE TABLE `dax_component_argument_maps` (
  `seed_id` varchar(64) NOT NULL,
  `dax_id` varchar(64) NOT NULL default '',
  `job_id` varchar(64) NOT NULL,
  `component_id` varchar(128) default NULL,
  `component_type` varchar(128) default NULL,
  `input_maps` longblob,
  `output_maps` longblob,
  PRIMARY KEY  (`job_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;;

--
-- Table structure for table `instances_and_daxes`
--

DROP TABLE IF EXISTS `instances_and_daxes`;
CREATE TABLE `instances_and_daxes` (
  `seed_id` varchar(64) NOT NULL default '',
  `instance_id` varchar(64) NOT NULL default '',
  `dax_id` varchar(64) NOT NULL default '',
  `instance` text,
  `dax` text,
  PRIMARY KEY  (`instance_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `bound_workflows`
--

DROP TABLE IF EXISTS `bound_workflows`;
CREATE TABLE `bound_workflows` (
  `seed_id` varchar(64) NOT NULL default '',
  `instance_id` varchar(64) NOT NULL default '',
  `instance` text,
  PRIMARY KEY  (`instance_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `query_2_point_1`
--

DROP TABLE IF EXISTS `query_2_point_1`;
CREATE TABLE `query_2_point_1` (
  `seed_id` varchar(64) NOT NULL default '',
  `query_id` varchar(64) NOT NULL default '',
  `query` text NOT NULL,
  `result` text NOT NULL,
  PRIMARY KEY  (`query_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `query_3_point_1`
--

DROP TABLE IF EXISTS `query_3_point_1`;
CREATE TABLE `query_3_point_1` (
  `seed_id` varchar(64) NOT NULL default '',
  `query_id` varchar(64) NOT NULL default '',
  `query` text NOT NULL,
  `result` text NOT NULL,
  PRIMARY KEY  (`query_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `query_4_point_5`
--

DROP TABLE IF EXISTS `query_4_point_5`;
CREATE TABLE `query_4_point_5` (
  `seed_id` varchar(64) NOT NULL default '',
  `query_id` varchar(64) NOT NULL default '',
  `query` text NOT NULL,
  `result` text NOT NULL,
  PRIMARY KEY  (`query_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `seed_data`
--

DROP TABLE IF EXISTS `seed_data`;
CREATE TABLE `seed_data` (
  `seed_id` varchar(64) NOT NULL,
  `data_object_id` varchar(64) NOT NULL 
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

--
-- Table structure for table `candidate_workflows`
--

DROP TABLE IF EXISTS `candidate_workflows`;
CREATE TABLE `candidate_workflows` (
  `seed_id` varchar(64) NOT NULL default '',
  `template_id` varchar(64) NOT NULL default '',
  `template` text,
  PRIMARY KEY  (`template_id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8; 

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2008-03-28 19:52:19
