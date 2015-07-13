*Note: this project is in pre-alpha state, so below instructions may not work completely yet*

easy-stage-dataset
==================

Stage a dataset in DANS-bagIt format for ingest into an EASY Fedora Commons Repository


SYNOPSIS
--------

    easy-stage-dataset [-e|--external-bagit-archive] <DANS-bagIt directory> <SDO-set directory>


DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-deposit] service. These
deposits must conform to the [EASY-bagIt] format, which is basically [bagIt] with some extra EASY-specific requirements.

To convert this deposit into an EASY dataset it is first converted into a *Staged Digital Object Set* (an [SDO-set]) which can
then be ingested into a Fedora Commons 3.x repository using the [easy-ingest] command.








[easy-deposit]: https://github.com/DANS-KNAW/easy-deposit
[EASY-bagIt]: 
[bagIt]: https://tools.ietf.org/html/draft-kunze-bagit-10
[SDO-set]: https://github.com/DANS-KNAW/easy-ingest#staged-digital-object-set
[easy-ingest]: https://github.com/DANS-KNAW/easy-ingest
