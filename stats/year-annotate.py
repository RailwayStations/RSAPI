#!/usr/bin/env python3
# Author: Kees Cook <kees@kernel.org>
# License: MIT
# Source from: 
# https://github.com/kees/kernel-tools/blob/trunk/stats/year-annotate.py
# Reworked from the original Perl implementation:
# https://github.com/curl/stats/blob/master/codeage.pl
# Adapted to RSAPI by: Peter Storch
#
# Extract per-line commit dates for each git commit for graphing code ages.
# Inspired by:
# https://fosstodon.org/@bagder@mastodon.social/113399049650160188
# https://github.com/curl/stats/blob/master/codeage.pl
# https://github.com/curl/stats/blob/master/codeage.plot
#
# cd ~/git/RSAPI && stats/year-annotate.py -d | tee codeage.csv
import glob, sys, os
import json
import pickle
import optparse
import datetime
from subprocess import *
from itertools import repeat
from multiprocessing import Pool, cpu_count
import tqdm
from packaging.version import Version
from pathlib import Path

# Globals (FIXME: turn this into a proper object)
parser = optparse.OptionParser()
parser.add_option("-d", "--debug", help="Report additional debugging while processing USNs", action='store_true')
parser.add_option("-C", "--csv", help="Report as CSV file", action='store_true')
(opt, args) = parser.parse_args()

devnull = open("/dev/null", "w")

cache_dir = os.path.expanduser("~/.cache/codeage")
Path(cache_dir).mkdir(parents=True, exist_ok=True)

def save_cache(cache, commit):
    cachefile = "%s/%s.pickle" % (cache_dir, commit)
    if opt.debug:
        print("Saving cache %s ..." % (cachefile), file=sys.stderr)
    pickle.dump(cache, open(cachefile, 'wb'), -1)

def load_cache(commit):
    cachefile = "%s/%s.pickle" % (cache_dir, commit)
    if os.path.exists(cachefile):
        if opt.debug:
            print("Loading cache %s ..." % (cachefile), file=sys.stderr)
        cache = pickle.load(open(cachefile, 'rb'))
    else:
        cache = dict()
        cache.setdefault('annotated', dict())
        cache.setdefault('ages', dict())
    return cache

def run(cmd):
    return Popen(cmd, stdout=PIPE, stderr=devnull).communicate()[0].decode("utf-8", "ignore")

def sha_to_date(sha):
    output = run(["git", "show", "--pretty=%at", "-s", sha])
    epoch = output.strip()
    date = datetime.datetime.fromtimestamp(float(epoch))
    return date

def annotate(commit, file):
    epochs = dict()
    ann = run(['git', 'annotate', '-t', '--line-porcelain', file, commit]).splitlines()
    for line in ann:
        if line.startswith('committer-time '):
            epoch = int(line.split(' ', 1)[1])
            epochs.setdefault(epoch, 0)
            epochs[epoch] += 1
    return {file: epochs}

def frombefore(before, epochs):
    count = 0
    for file in epochs:
        for epoch in epochs[file]:
            if epoch < before:
                count += epochs[file][epoch]
    return count

def process(commit, years):
    cache = load_cache(commit)

    date = sha_to_date(commit)
    epochs = cache['annotated']
    if len(epochs) == 0:
        if opt.debug:
            print(date.strftime('Processing files at %%s (%Y-%m-%d) ...') % (commit), file=sys.stderr)
        # Do we want to exclude Documentation, samples, or tools subdirectories?
        # Or MAINTAINERS, dot files, etc?
        files = run(['git', 'ls-tree', '-r', '--name-only', commit]).splitlines()
        count = len(files)

        with Pool(cpu_count()) as p:
            results = p.starmap(annotate,
                                tqdm.tqdm(zip(repeat(commit), files), total=count))
                                #zip(repeat(commit), files))
            # starmap produces a list of outputs from the function.
            for result in results:
                epochs |= result

        cache['annotated'] = epochs
        # Save this commit's epochs!
        save_cache(cache, commit)


    report = cache['ages']
    if len(report) == 0:
        day = date.strftime('%Y-%m-%d')
        report = day
        if opt.debug:
            print('Scanning ages ...                  ', file=sys.stderr)
        for year in years:
            report += ';%u' % (frombefore(year, epochs))
        # Save age span report
        cache['ages'] = report
        save_cache(cache, commit)
    print(report)

# Get the list of commits we're going to operate against
commits = run(["git", "log", "--pretty=format:%H", "--reverse"]).splitlines()
if opt.debug:
    print(commits, file=sys.stderr)

# Find the year bounds of our commit
year_first = sha_to_date(commits[0]).year + 1
year_last  = sha_to_date(commits[-1]).year + 1
if opt.debug:
    print("%d .. %d" % (year_first, year_last), file=sys.stderr)
years = [int(datetime.datetime.strptime('%d' % (year), '%Y').strftime("%s")) for year in range(year_first, year_last + 1, 1)]
if opt.debug:
    print(years, file=sys.stderr)

# Walk commits
for commit in commits:
    process(commit, years)
