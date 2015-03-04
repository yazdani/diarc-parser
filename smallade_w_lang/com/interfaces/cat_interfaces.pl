#!/usr/bin/perl

# Concatenate individual chapters into an entire tex file.
# The idea is that each chapter can be compiled individually,
# which makes each easier to work on. This automates combining
# all the chapters into a single file.
# 
# Assume that the appropriate tex header (eg, docclass,
# newcommands, etc) are in a file called "header.tex",
# that every line between "\begin{document}" and "\bibliography"
# in the individual chapter files can be copied verbatim, and
# that the closing lines for tex are contained in a file called
# "footer.tex".

$opfile = "everything.tex";
#if ($#ARGV < 0 || $#ARGV > 1) {
#	&usage();
#}
open(O_FI, "> $opfile") || die "Error creating $opfile\n";
print "Creating $opfile...\n";
&catfile("./header.tex");
&cattexfile("./intro.tex");
&cattexfile("./background.tex");
&cattexfile("./systems.tex");
&cattexfile("./agesfunc.tex");
&cattexfile("./agesimpl.tex");
&catfile("./footer.tex");

sub usage() {
	print "Usage: cattex.pl <outfile>\n";
	exit(0);
}

sub catfile() {
	my ($fi) = (@_);
	open(I_FI, "<", $fi) || die "Error opening $fi\n";
	print "Copying $fi...";
	while (<I_FI>) {
		print O_FI $_;
	}
	print "done.\n";
}

sub cattexfile() {
   my ($fi)  = (@_);
	my $copylines = "no";
   open(I_FI, "<", $fi) || die "Error opening $fi\n";
	print "Copying content of $fi...";
   LINE: while (<I_FI>) {
		if (/^\\begin{document}/) {
			$copylines = "yes";
			next LINE;
		}
      next LINE if $copylines eq "no";
		if (/^\\bibliographystyle/) {
			print "done.\n";
			return;
		}
		print O_FI $_;
	}
	print "hit end of file; bad format?\n";
}
