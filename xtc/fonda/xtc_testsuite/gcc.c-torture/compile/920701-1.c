f(char*c){extern char a[],b[];return a+(b-c);} // {{ dg-warning "without" }}
