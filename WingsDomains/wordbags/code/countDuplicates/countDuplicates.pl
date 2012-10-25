#!/usr/bin/perl

my $threshold = int($ARGV[1]);

open(FILE, $ARGV[0]);
my $last = "";
my $count = 0;
while (<FILE>) {
    my $line = $_;
    $line =~ s/\n//;
    if ($line eq $last) {
	$count = $count + 1;
    } else {
	if ($count>$threshold) {
	    print "$last $count\n";
	}
	$last = $line;
	$count = 1;
    }
}
close(f);
