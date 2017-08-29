#!/usr/bin/env bash

DATADIR=data

echo "Copying test data to $DATADIR..."
cp -r src/test/resources/deposits $DATADIR/deposits

touch $DATADIR/easy-bag-index.log
echo "OK"
