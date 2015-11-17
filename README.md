easy-stage-dataset
==================

Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.


SYNOPSIS
--------

    easy-stage-dataset -t <submission-timestamp> -u <urn> -d <doi> [ -o ] \
                          <EASY-bag> <staged-digital-object-set>

    easy-stage-file-item [<options>...] <staged-digital-object-set>
    easy-stage-file-item <staged-digital-object-set> <csv-file>


DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-deposit] service. These
deposits must conform to [BagIt] format.

To prepare the deposit for inclusion in EASY `easy-stage-dataset` performs the following tasks:

1. It generates the metadata required for an EASY dataset:
   * Administrative Metadata
   * EASY Metadata (descriptive metadata)
   * ...
2. It stages a digital object to represent the entire dataset for ingest in Fedora, using the metadata generated in 1.
3. It stages a digital object for each file and folder in the dataset for ingest in Fedora.

The results of steps 1-3 can be ingested into the EASY Fedora Commons Repository.
The command `easy-stage-file-item` executes step 3 to stage a file for ingestion into an existing dataset.


ARGUMENTS for easy-stage-dataset
--------------------------------

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


ARGUMENTS for easy-stage-fileItem
---------------------------------

     -c, --created  <arg>       dcterms property, date-time when the file was created
     -i, --dataset-id  <arg>    id of the dataset in Fedora that should receive the
                                file to stage (requires file-path). If omitted the
                                trailing argument csf-file is required
     -d, --description  <arg>   dcterms property description
     -f, --file  <arg>          File to stage for ingest into Fedora, if omitted a
                                folder is staged (requires dataset-id, md5 and
                                format)
     -p, --file-path  <arg>     the path that the file should get in the dataset or
                                the folder that should be created in the dataset
         --format  <arg>        dcterms property format, the mime type of the file
     -u, --identifier  <arg>    dcterms property
     -m, --md5  <arg>           MD5 checksum of the file to stage
     -t, --title  <arg>...      dcterms property title and optional alternatives
         --help                 Show help message
         --version              Show version of this program
   
    trailing arguments:
     staged-digital-object-set (required)   The resulting Staged Digital Object
                                            directory (will be created if it does not
                                            exist)
     csv-file (not required)                a comma separated file with one column
                                            for each option (additional columns are
                                            ignored) and one set of options per line


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

Run `./stageFileItem.sh out/file-sdos src/test/resources/example.csv`
to cover the logic that checks existence of datasets and folders in the repository or folder-SDO's on the file system.
Reading a CSV is also covered with a unit-test, creation of file/folder SDO's is covered by stageDataset.


Steps for a regression test:

* Create `apphome.sh` in the project root to override the var `APPHOME`
  or create a `home` folder in the root of the project.
  See also [installation and configuration](#installation-and-configuration).
* Remove the content of the directory `out` (ignored by git like the above)
* build with maven
* Process any example-bag you may find, for example:
  * `./stageDataset.sh -t2015 -uURN -dDOI src/test/resources/example-bag out/local-sdo`
  * `./stageDataset.sh -t2015 -uURN -dDOI ../easy-deposit/src/test/resources/simple/example-bag out/simple-sdo`
* Move the created `out` to another location, say `~/old`.
* Repeat until OK:
  * Clear the folders in `out`.
  * Process the same bags with the new code.
  * Compare the results: `diff -r ~/old target/test-out`


[dans-parent]: https://github.com/DANS-KNAW/dans-parent#dans-parent
[easy-deposit]: https://github.com/DANS-KNAW/easy-deposit#easy-deposit
[BagIt]: https://tools.ietf.org/html/draft-kunze-bagit-11

