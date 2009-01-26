#!/bin/bash

function check_continue
{
    echo >/dev/stderr
    set +e
    read -p "Do you want to continue? [n] " -t 120 YESNO
    if [ $? -ne 0 ]; then
        echo "...Aborting. (Timeout?)" >/dev/stderr
        exit 1;
    fi
    set -e
    if [ "$YESNO" != "y" -a "$YESNO" != "Y" ]; then
        echo "Aborting." >/dev/stderr
        exit 1;
    fi
    echo "Continuing." >/dev/stderr
    echo >/dev/stderr
}

set -e

ENKI_DIR=$(cd `dirname $0`; pwd)
cd $ENKI_DIR

# Warn if files are open
OPEN_FILES=`p4 -s opened ... | grep -E "^info: " | sed -e "s/info: \([^#]\+\)#.*/\1/"`

if [ "$OPEN_FILES" != "" ]; then
    cat >/dev/stderr <<-EOD
	**********************************************************************
	Warning: The following files are currently open. This script embeds
	information on the the latest changelist synchronized to current 
	client's source tree (as determined by client mappings) into the
	distribution. The changelist may be incorrect if you do not first
	submit your open files!
	**********************************************************************
	EOD

    echo "Open files:" >/dev/stderr
    for i in $OPEN_FILES; do
        echo -n "    " >/dev/stderr
        p4 where "$i" | awk '{print $NF}' >/dev/stderr
    done

    check_continue
fi

# Get latest change number
LAST_CHANGE=`p4 -s changes -i -m 1 -s submitted ... | grep -E "^info: " | sed -e "s/info: Change \([0-9]\+\) .*/\1/"`

# Get latest synced change number:
# Get current client's file list (for Enki)
#   Strip off client's mapped file names
#   Invoke p4 changes on the files (get last changelist for each file)
#   Strip everything but change numbers
#   Sort in reverse order and return the first one
LAST_SYNCED=`
    p4 have ... | 
    sed -e "s/ - .*//" | 
    xargs "--delimiter=\n" p4 changes -i -m 1 -s submitted | 
    sed -e "s/Change \([0-9]\+\) .*/\1/" | 
    sort -n -r | 
    head -n 1`

if [ $LAST_CHANGE -ne $LAST_SYNCED ]; then
    cat >>/dev/stderr <<-EOD
	**********************************************************************
	Warning: The lastest change on this source tree appears to be $LAST_CHANGE,
	but the latest change synchronized on this source tree appears to be
	$LAST_SYNCED. Note that partially synchronized changes cannot be
	detected.  Also note that if the last change contained only deletes,
	it will not be detected and the wrong change number will be embedded
	into the build.

	Change $LAST_SYNCED will be embedded into the build.
	**********************************************************************
EOD

    check_continue
fi

BRANCH=`p4 have ./build.xml | sed -e "s@\(.*\)/build.xml#.*@\1@"`

DIST_DIR="$ENKI_DIR/dist"

DETAIL_FILE="$DIST_DIR/LAST_CHANGE"

echo "Embedding details for ${BRANCH}@${LAST_SYNCED}"
echo "--"

echo >"$DETAIL_FILE" ${BRANCH}@${LAST_SYNCED}
echo >>"$DETAIL_FILE" "--"

p4 describe -s $LAST_SYNCED >>"$DETAIL_FILE"

ant "-Ddist.build.details=${DETAIL_FILE}" dist

echo "Done."
