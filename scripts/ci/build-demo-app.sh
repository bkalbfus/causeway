#!/bin/bash
set -e

SCRIPT_DIR=$( dirname "$0" )
if [ -z "$PROJECT_ROOT_PATH" ]; then
  PROJECT_ROOT_PATH=`cd $SCRIPT_DIR/../.. ; pwd`
fi

if [ -z "$REVISION" ]; then
  if [ ! -z "$SHARED_VARS_FILE" ] && [ -f "$SHARED_VARS_FILE" ]; then
    . $SHARED_VARS_FILE
    export $(cut -d= -f1 $SHARED_VARS_FILE)
  fi
fi
if [ -z "$MVN_STAGES" ]; then
  MVN_STAGES="clean install"
fi

sh $SCRIPT_DIR/print-environment.sh "build-demo-app"

export FLAVOR=$1
export ISIS_VERSION=$REVISION
echo ""
echo "\$Docker Image Flavor: ${FLAVOR}"
echo "\$Isis Version: ${ISIS_VERSION}"
echo ""

#
# update version (but just for the modules we need to build)
#
cd $PROJECT_ROOT_PATH/core-parent

if [ -z "$REVISION" ]; then
  REVISION=$(grep "<version>" pom.xml | head -1 | cut -d">" -f2 | cut -d"<" -f1)
fi

mvn versions:set -DnewVersion=$REVISION -Drevision=$REVISION -Ddemo-app-modules

cd $PROJECT_ROOT_PATH

#
# now build the apps
#
for app in demo
do
  cd $PROJECT_ROOT_PATH/examples/apps/$app

  mvn --batch-mode \
      -Drevision=$REVISION \
      clean install \
      -Dflavor=$FLAVOR \
      -Dskip.git \
      -Dskip.arch \
      -DskipTests

  mvn --batch-mode \
      -Drevision=$REVISION \
      compile jib:build \
      -Dflavor=$FLAVOR \
      -Dskip.git \
      -Dskip.arch \
      -DskipTests

  cd $PROJECT_ROOT_PATH
done

#
# finally, revert the version
#
cd $PROJECT_ROOT_PATH/core-parent
mvn versions:revert -Drevision=$REVISION -Dstarter-apps-modules
cd $PROJECT_ROOT_PATH
