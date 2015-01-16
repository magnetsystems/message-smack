#!/bin/bash
#
# Rename every smack-*.jar to mmx-smack-*.jar in the distribution zip file.
#

SMACK_ZIP=`ls build/distributions | tail -1`
if [ -z "${SMACK_ZIP}" ]; then
  echo "No smack zip found in build/distributions. Please run 'gradle distributionZip' first."
  exit 1
fi

TMPDIR=build/.tmp
test -d ${TMPDIR} && rm -rf ${TMPDIR}
mkdir ${TMPDIR}
unzip -qq build/distributions/${SMACK_ZIP} -d ${TMPDIR}
( cd ${TMPDIR}/libs;
  for jarfile in smack-*.jar; do
    mv ${jarfile} mmx-${jarfile}
  done
)
( cd ${TMPDIR}; rm -f ../distributions/${SMACK_ZIP}; zip -q -r ../distributions/${SMACK_ZIP} . )
rm -rf ${TMPDIR}
echo "Distributions zip build/distributions/${SMACK_ZIP} is repackaged for MMX."
