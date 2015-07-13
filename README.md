*Note: this project is in pre-alpha state, so below instructions may not work completely yet*

easy-stage-dataset
==================

Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.


SYNOPSIS
--------

    easy-stage-dataset [-e <url> [-c]] <EASY-BagIt directory> <SDO-set directory>


DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-deposit] service. These
deposits must conform to the [EASY-BagIt] format, which is basically [BagIt] with some extra EASY-specific requirements.

To prepare the deposit for inclusion in EASY ``easy-stage-dataset`` performs the following tasks:

1. It generates the metadata required for an EASY dataset:
   * Adminstrative Metadata
   * EASY Metadata (descriptive metadata)
   * ...
2. It stages a digital object to represent the entire dataset for ingest in Fedora, using the metadata generated in 1.
3. It stages a digital object for each file and folder in the dataset for ingest in Fedora.
4. It generates an SQL-script to add the files and folders to the EASY Filesystem RDB.
5. It generates a SOLR document to add the dataset to the EASY SOLR Search Index.

The results of steps 1-3 can be ingested into the EASY Fedora Commons Repository. The result of 4 can be executed by 
PostGreSQL and the result of 5 will be accepted by the SOLR service.


ARGUMENTS
---------

* ``-e``, ``--external-bagit-archive-url`` -- in case the dataset data is stored in Fedora-external storage, the URL of the   
   directory where it is stored. ``easy-stage-dataset`` will create Redirect datastreams for all the data files in the bag.
* ``-c``, ``--check-data-file-existence-in-storage`` -- if set ``easy-stage-dataset`` will do an http ``HEAD`` request on each
   of the data files in the bag to ensure that it exists in archival storage. Can only be specified if ``-e`` is also used.
* ``<EASY-BagIt directory>`` -- a directory conforming to the [EASY-BagIt] format.
* ``<SDO-set directory>`` -- the SDO-set directory to put the generated [SDOs] in. If the directory does not exist it is first
  created.


INSTALLATION AND CONFIGURATION
------------------------------

### Installation steps:

1. Unzip the tarball to a directory of your choice, e.g. /opt/
2. A new directory called easy-stage-dataset-<version> will be created
3. Create an environment variabele ``EASY_STAGE_DATASET_HOME`` with the directory from step 2 as its value
4. Add ``$EASY_STAGE_DATASET_HOME/bin`` to your ``PATH`` environment variable.


### Configuration

General configuration settings can be set in ``EASY_STAGE_DATASET_HOME/cfg/application.properties`` and logging can be
configured in ``EASY_STAGE_DATASET_HOME/cfg/logback.xml``. The available settings are explained in comments in 
aforementioned files.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 7 or higher
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


[easy-deposit]: https://github.com/DANS-KNAW/easy-deposit#easy-deposit
[easy-ingest]: https://github.com/DANS-KNAW/easy-ingest#easy-ingest
[EASY-BagIt]: http://easy.dans.knaw.nl/schemas/EASY-BagIt.html 
[SDO-set]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-object-set
[SDOs]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-objects

[BagIt]: https://tools.ietf.org/html/draft-kunze-bagit-10

