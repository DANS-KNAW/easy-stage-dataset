easy-stage-dataset
==================

Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.


SYNOPSIS
--------

    easy-stage-dataset -t <submission-timestamp> -u <urn> -d <doi> [ -o ] \
                          <EASY-bag> <staged-digital-object-set>

DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-deposit] service. These
deposits must conform to the [EASY-BagIt] format, which is basically [BagIt] with some extra EASY-specific requirements.

To prepare the deposit for inclusion in EASY `easy-stage-dataset` performs the following tasks:

1. It generates the metadata required for an EASY dataset:
   * Administrative Metadata
   * EASY Metadata (descriptive metadata)
   * ...
2. It stages a digital object to represent the entire dataset for ingest in Fedora, using the metadata generated in 1.
3. It stages a digital object for each file and folder in the dataset for ingest in Fedora.

The results of steps 1-3 can be ingested into the EASY Fedora Commons Repository.


ARGUMENTS
---------

     -d, --doi  <arg>                    The DOI to assign to the new dataset in EASY
     -o, --doi-is-other-access-doi       Stage the provided DOI as an "other access
                                         DOI"
     -t, --submission-timestamp  <arg>   Timestamp in ISO8601 format
     -u, --urn  <arg>                    The URN to assign to the new dataset in EASY
         --help                          Show help message
         --version                       Show version of this program
   
    trailing arguments:
     EASY-bag (required)                    Bag with extra metadata for EASY to be
                                            staged for ingest into Fedora
     staged-digital-object-set (required)   The resulting Staged Digital Object
                                            directory (will be created if it does not
                                            exist)

INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps:

1. Unzip the tarball to a directory of your choice, e.g. `/opt/`
2. A new directory called easy-stage-dataset-<version> will be created
3. The directory from step 2 is used as value for the system property ``app.home``
4. Add ``${app.home}/bin`` to your ``PATH`` environment variable


### Configuration

General configuration settings can be set in `${app.home}/cfg/application.properties` and logging can be
configured in `${app.home}/cfg/logback.xml`. The available settings are explained in comments in 
aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Scala 2.11 or higher
* Maven 3.3.3 or higher
 
Steps:

1. Clone and build the [dans-parent] project (*can be skipped if you have access to the DANS maven repository*)
      
        git clone https://github.com/DANS-KNAW/dans-parent.git
        cd dans-parent
        mvn install
2. Clone and build this project

        git clone https://github.com/DANS-KNAW/easy-stage-dataset.git
        cd easy-stage-dataset
        mvn install

[dans-parent]: https://github.com/DANS-KNAW/dans-parent#dans-parent
[easy-deposit]: https://github.com/DANS-KNAW/easy-deposit#easy-deposit
[EASY-BagIt]: http://easy.dans.knaw.nl/schemas/EASY-BagIt.html 
[BagIt]: https://tools.ietf.org/html/draft-kunze-bagit-11

