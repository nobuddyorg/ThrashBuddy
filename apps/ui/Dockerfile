FROM node:26@sha256:6d2c7d5d074bf72f81ddc252b4fbb5d625bc028eeda49cb4b8c2ff8eda1ee617 AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm install
COPY . .
RUN npx ng build --configuration production

FROM nginx:alpine
# Pick up Alpine security fixes that land before the next nginx:alpine rebuild.
RUN apk upgrade --no-cache
COPY --from=build /app/dist/thrash-buddy/browser /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
