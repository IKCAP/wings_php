#!/usr/bin/perl

## $1: a file from whcih to remove any line that matches any pattern defined in $2 (which is a list of patterns, one per line)

use strict;

my @patterns;
open(FILE, $ARGV[1]);
while (<FILE>) {
    push(@patterns, $_);
}
close(FILE);

open(FILE, $ARGV[0]);
while (<FILE>) {
    my $line = $_;
    my $print = 1;
    foreach (@patterns) {
	if ($line =~ $_) {
	    $print = 0;
	    next;
	}
    }
    if ($print) {
	print $line;
    }
}
close(FILE);
