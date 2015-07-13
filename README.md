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
deposits must conform to the [EASY-BagIt] format, which is basically [bagIt] with some extra EASY-specific requirements.

To convert this deposit into an EASY dataset it is first converted into a *Staged Digital Object Set* (an [SDO-set]) which can
then be ingested into a Fedora Commons 3.x repository using the [easy-ingest] command.


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






[easy-deposit]: https://github.com/DANS-KNAW/easy-deposit
[EASY-BagIt]: http://easy.dans.knaw.nl/schemas/EASY-BagIt.html 
[bagIt]: https://tools.ietf.org/html/draft-kunze-bagit-10
[SDO-set]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-object-set
[easy-ingest]: https://github.com/DANS-KNAW/easy-ingest#easy-ingest
[SDOs]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-objects
