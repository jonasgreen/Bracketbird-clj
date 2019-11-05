# Recontain


### Vertical overriding

In vertical overriding the same local state is used.


##### Component inheritance

Happens when a component is created (optimize: or when config is loaded).

##### Element inheritance

Happens when an element is created.


Component <- Component-Parents <- Element options <- Element-parents

This results configuration for that element.


### Horizontal overriding

Is when one component overrides a child components config of a specific 
element. Local state is bound to the component that effectively runs 
the configuration of the element.

Configuration regarding a child component is passed from parent 
component to child component upon child creation.





