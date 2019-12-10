#!/usr/bin/env bash
#
# Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


HOMEDIR=home
DATADIR=data

echo "Copying test data to $DATADIR..."
cp -r src/test/resources/deposits $DATADIR/deposits

echo "Copying licenses to $HOMEDIR/cfg..."
mvn generate-resources
LICENSES=target/easy-licenses/licenses
cp -r "$LICENSES" $HOMEDIR/cfg/lic

touch $DATADIR/easy-stage-dataset.log
echo "OK"
