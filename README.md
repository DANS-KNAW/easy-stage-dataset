easy-stage-dataset
==================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-stage-dataset.svg?branch=master)](https://travis-ci.org/DANS-KNAW/easy-stage-dataset)

Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.


SYNOPSIS
--------

    easy-stage-dataset -t <submission-timestamp> -u <urn> -d <doi> [ -o ] [ -m ] \
                          <EASY-bag> <staged-digital-object-set>

    easy-stage-file-item [<options>...] <staged-digital-object-set>


DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-sword2] service. These
deposits must conform to [BagIt] format.

To prepare the deposit for inclusion in EASY `easy-stage-dataset` performs the following tasks:

 1. It generates the metadata required for an EASY dataset:
    * Administrative Metadata
    * EASY Metadata (descriptive metadata)
    * ...
 2. It stages a digital object to represent the entire dataset for ingest in Fedora, using the metadata generated in 1.
 3. It stages a digital object for each file and folder in the dataset for ingest in Fedora.

The results of steps 1-3 can be ingested into the EASY Fedora Commons Repository. See the wiki section
about the [export-import] cycle as `easy-export-dataset` also creates Staged Digital Objects.

References to files in external storage such as audio/video files can be added to existing datasets with 
the command `easy-stage-file-item`. It executes step 3 to stage one or more files for ingestion.


ARGUMENTS for easy-stage-dataset
--------------------------------

    -d, --doi  <arg>                                The DOI to assign to the new dataset in EASY
    -o, --doi-is-other-access-doi                   Stage the provided DOI as an "other access DOI"
    -r, --stage-file-data-as-redirect-datastreams   Stage file items so that the content of file data will NOT
                                                    be stored in managed Fedora Storage
    -t, --submission-timestamp  <arg>               Timestamp in ISO8601 format
    -u, --urn  <arg>                                The URN to assign to the new dataset in EASY
        --help                                      Show help message
        --version                                   Show version of this program

    trailing arguments:
     EASY-deposit (required)                Deposit directory contains deposit.properties file and bag with extra
                                           metadata for EASY to be staged for ingest into Fedora
        staged-digital-object-set (required)   The resulting Staged Digital Object directory (will be created if it
                                           does not exist)


ARGUMENTS for easy-stage-fileItem
---------------------------------

    -i, --dataset-id  <arg>            id of the dataset in Fedora that should receive the file to stage
                                       (requires file-path). If omitted the trailing argument csv-file is
                                       required
    -d, --datastream-location  <arg>   http URL to redirect to (if specified, file-location MUST NOT be
                                       specified)
    -p, --path-in-dataset  <arg>       the path that the file should get in the dataset, a staged digital object
                                       is created for the file and the ancestor folders that don't yet exist in
                                       the dataset
    -s, --size  <arg>                  Size in bytes of the file data
    -f, --format  <arg>                dcterms property format, the mime type of the file
                                       (default = application/octet-stream)
  
    -a, --accessible-to  <arg>         specifies the accessibility of the file item; one of: ANONYMOUS, KNOWN,
                                       RESTRICTED_REQUEST, RESTRICTED_GROUP, NONE (default = NONE)
    -c, --creator-role  <arg>          specifies the role of the file item creator; one of: ARCHIVIST, DEPOSITOR
                                       (default = DEPOSITOR)
        --csv-file  <arg>              a comma separated file with one column for each option (additional
                                       columns are ignored) and one set of options per line
    -l, --file-location  <arg>         The file to be staged (if specified, --datastream-location is ignored)
        --owner-id  <arg>              specifies the id of the owner/creator of the file item (defaults to the
                                       one configured in the application configuration file)
    -v, --visible-to  <arg>            specifies the visibility of the file item; one of: ANONYMOUS, KNOWN,
                                       RESTRICTED_REQUEST, RESTRICTED_GROUP, NONE (default = ANONYMOUS)
        --help                         Show help message
        --version                      Show version of this program
  
    trailing arguments:
     staged-digital-object-sets (required)   The resulting directory with Staged Digital Object directories per
                                            dataset (will be created if it does not exist)

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

* Java 8 or higher
* Maven 3.3.3 or higher
 
Steps:

        git clone https://github.com/DANS-KNAW/easy-stage-dataset.git
        cd easy-stage-dataset
        mvn install

TEST NOTES
----------

No tests are available that mock fedora or the database for files and folders.
So run integration tests to check for regression.

Run `./stageFileItem.sh src/test/resources/example.csv out/file-sdos`
to cover the logic that checks existence of datasets and folders in the repository or folder-SDO's on the file system.
Reading a CSV is also covered with a unit-test, creation of file/folder SDO's is covered by stageDataset.


Steps for a regression test:

* Create `apphome.sh` in the project root to override the var `APPHOME`
  or create a `home` folder in the root of the project.
  See also [installation and configuration](#installation-and-configuration).
* Remove the content of the directory `out` (ignored by git like apphome.sh and home)
* use maven to compile a version of the code without the changes under test
* Process any example-bag you may find, for example:
  * `./stageDataset.sh -t2015 -uURN -dDOI src/test/resources/example-bag out/local-sdo`
  * `./stageDataset.sh -t2015 -uURN -dDOI ../easy-sword2/src/test/resources/simple/example-bag out/simple-sdo`
* Move the created `out` to another location, say `~/old`.
* Make your changes to the code and repeat until OK:
  * compile the new code with maven
  * Clear the folders in `out`.
  * Process the same bags.
  * Compare the results: `diff -r ~/old target/test-out` and check for changes you did not intend


[dans-parent]: https://github.com/DANS-KNAW/dans-parent#dans-parent
[easy-sword2]: https://github.com/DANS-KNAW/easy-sword2#easy-sword2
[BagIt]: https://tools.ietf.org/html/draft-kunze-bagit-11
[export-import]: https://github.com/DANS-KNAW/easy-export-dataset/wiki#the-export-import-cycle
