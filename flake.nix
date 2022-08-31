{
  description = "a kotlin implementation of the lox programming language";

  inputs = {
    flake-utils.url = "github:numtide/flake-utils";
    gradle2nix.url = "github:michzappa/gradle2nix";
    nixpkgs.url = "github:NixOs/nixpkgs/nixpkgs-unstable";
  };

  outputs = { self, ... }@inputs:
    with inputs;
    flake-utils.lib.eachDefaultSystem (system:
      let
        buildGradle = pkgs.callPackage ./gradle-env.nix { };
        graalvm = pkgs.graalvm17-ce;
        pkgs = (import nixpkgs { inherit system; });
      in {
        devShell = pkgs.mkShell {
          buildInputs = with pkgs; [
            graalvm
            gradle
            inputs.gradle2nix.packages.${system}.default
            kotlin
            kotlin-language-server
            ktlint
          ];
          shellHook = ''
            export GRAALVM_HOME=${graalvm};
          '';
        };

        packages = rec {
          default = jvm;
          jvm = buildGradle {
            envSpec = ./gradle-env.json;
            gradleFlags = [ "installDist" ];
            installPhase = ''
              mkdir $out
              cp -r app/build/install/klox/* $out
            '';
            pname = "klox";
            src = ./.;
            version = "jvm";
          };
          native = buildGradle {
            configurePhase = ''
              export GRAALVM_HOME=${graalvm};
            '';
            envSpec = ./gradle-env.json;
            gradleFlags = [ "nativeCompile" ];
            installPhase = ''
              mkdir -p $out/bin
              cp app/build/native/nativeCompile/klox $out/bin
            '';
            nativeBuildInputs = [ graalvm ];
            pname = "klox";
            src = ./.;
            version = "native";
          };
        };
      });
}
