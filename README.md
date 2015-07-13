*Note: this project is in pre-alpha state, so below instructions may not work completely yet*

easy-stage-dataset
==================

Stage a dataset in DANS-bagIt format for ingest into an EASY Fedora Commons Repository


SYNOPSIS
--------

    easy-stage-dataset <DANS-bagIt directory>


DESCRIPTION
-----------

Datasets that are to be archived in EASY are initially received as *deposits* through the [easy-deposit] service. These
deposit must conform to the [EASY-bagIt] format, which is basically [bagIt] with some extra EASY-specific requirements
