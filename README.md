# gdx-csg

Library to perform [Constructive Solid Geometry (CSG)](https://en.wikipedia.org/wiki/Constructive_solid_geometry) operations on meshes that is specialized to work with libGDX.

## Limitations

  - CSG operations produce 1 mesh, thus if texture coordinates are used all meshes in the CSG operations must share the same texture
  - Does not support multiple mesh parts per meshes
  - It often fails due to floating point inaccuracy. Current state of the library is not totally reliable.

## Screenshots

![image](https://github.com/user-attachments/assets/5aa40ba2-4250-4f90-a341-b196566ecc86)
![image](https://github.com/user-attachments/assets/f3a6f310-1cb1-454e-862a-0596699b5df8)
